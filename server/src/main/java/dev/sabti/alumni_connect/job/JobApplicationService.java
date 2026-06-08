package dev.sabti.alumni_connect.job;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobApplicationRepository;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    // Applying requires: a real CandidateProfile behind the caller, an OPEN offer,
    // no pre-existing application from this candidate to this offer, and — if the
    // offer caps applications — room left under that cap. All collapse to one
    // "can't apply" outcome, the same Optional "soft failure" pattern as
    // registerCompanyMember/postJobOffer (the controller maps empty -> 400).
    @Transactional
    public Optional<JobApplication> apply(String applicantEmail, Long jobOfferId, ApplyToJobOfferDTO dto) {
        User user = userRepository.findByEmail(applicantEmail).orElse(null);
        if (user == null) return Optional.empty();

        CandidateProfile applicant = candidateProfileRepository.findByUser(user).orElse(null);
        if (applicant == null) return Optional.empty();

        JobOffer offer = jobOfferRepository.findById(jobOfferId).orElse(null);
        if (offer == null || offer.getStatus() != JobStatus.OPEN) return Optional.empty();

        if (jobApplicationRepository.existsByJobOfferAndApplicant(offer, applicant)) return Optional.empty();

        if (offer.getMaxApplications() != null
                && offer.getCurrentApplicationCount() >= offer.getMaxApplications()) {
            return Optional.empty();
        }

        JobApplication application = new JobApplication();
        application.setJobOffer(offer);
        application.setApplicant(applicant);
        application.setCoverLetter(dto.getCoverLetter());
        application = jobApplicationRepository.save(application);

        offer.setCurrentApplicationCount(offer.getCurrentApplicationCount() + 1);
        jobOfferRepository.save(offer);

        return Optional.of(application);
    }
}
