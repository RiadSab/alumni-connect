package dev.sabti.alumni_connect.job.saved;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.entities.SavedJob;
import dev.sabti.alumni_connect.job.offers.JobOfferDTO;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import dev.sabti.alumni_connect.job.repositories.SavedJobRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedJobService {
    private final SavedJobRepository savedJobRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    // Idempotent; only OPEN offers are saveable (others 404). See docs/saved-jobs.md.
    @Transactional
    public JobOfferDTO save(String candidateEmail, Long offerId) {
        CandidateProfile candidate = requireCandidate(candidateEmail);
        JobOffer offer = jobOfferRepository.findById(offerId)
                .filter(o -> o.getStatus() == JobStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));

        if (!savedJobRepository.existsByCandidateAndJobOffer(candidate, offer)) {
            SavedJob saved = new SavedJob();
            saved.setCandidate(candidate);
            saved.setJobOffer(offer);
            savedJobRepository.save(saved);
        }

        JobOfferDTO dto = JobOfferDTO.from(offer);
        dto.setIsSaved(true);
        return dto;
    }

    // Idempotent: unsaving a non-saved offer is a no-op.
    @Transactional
    public void unsave(String candidateEmail, Long offerId) {
        CandidateProfile candidate = requireCandidate(candidateEmail);
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));
        savedJobRepository.deleteByCandidateAndJobOffer(candidate, offer);
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getMySavedOffers(String candidateEmail, Pageable pageable) {
        CandidateProfile candidate = requireCandidate(candidateEmail);
        return savedJobRepository.findByCandidate(candidate, pageable).map(saved -> {
            JobOfferDTO dto = JobOfferDTO.from(saved.getJobOffer());
            dto.setIsSaved(true);
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<Long> getMySavedOfferIds(String candidateEmail) {
        CandidateProfile candidate = requireCandidate(candidateEmail);
        return savedJobRepository.findSavedOfferIds(candidate);
    }

    // Requires a user with a CandidateProfile; either missing -> 403.
    private CandidateProfile requireCandidate(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Only candidates can save jobs"));
        return candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Only candidates can save jobs"));
    }
}
