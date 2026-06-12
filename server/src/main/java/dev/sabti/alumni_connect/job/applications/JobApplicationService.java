package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobApplicationRepository;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;

    // Applying requires: a real CandidateProfile behind the caller, an OPEN offer,
    // no pre-existing application from this candidate to this offer, and — if the
    // offer caps applications — room left under that cap. All collapse to one
    // "can't apply" outcome, the same Optional "soft failure" pattern as
    // registerCompanyMember/postJobOffer (the controller maps empty -> 400).
    @Transactional
    public Optional<JobApplicationDTO> apply(String applicantEmail, Long jobOfferId, ApplyToJobOfferDTO dto) {
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

        return Optional.of(JobApplicationDTO.from(application));
    }

    // The candidate's own application history, any status. Empty -> 403 if the
    // caller has no CandidateProfile (this endpoint doesn't apply to them) — old
    // code instead used messy instanceof-principal checks and silently returned an
    // empty page for non-candidates.
    @Transactional(readOnly = true)
    public Optional<Page<JobApplicationDTO>> getMyApplications(String applicantEmail, Pageable pageable) {
        User user = userRepository.findByEmail(applicantEmail).orElse(null);
        if (user == null) return Optional.empty();

        CandidateProfile applicant = candidateProfileRepository.findByUser(user).orElse(null);
        if (applicant == null) return Optional.empty();

        return Optional.of(jobApplicationRepository.findByApplicant(applicant, pageable).map(JobApplicationDTO::from));
    }

    // Listing applicants is restricted to the OWNER/RECRUITER of the company that
    // posted THIS specific offer — same authority boundary as reviewing (see review()).
    // The optional triage filters (status / reviewed / minRating) only narrow within this
    // offer's applicants: forOffer(...) is the non-optional base, ANDed with whatever the
    // caller supplied. The authority check runs first, so filters never run for an outsider.
    @Transactional(readOnly = true)
    public Optional<Page<JobApplicationDTO>> getApplicationsForOffer(String reviewerEmail, Long jobOfferId,
                                                                     JobApplicationSearchCriteria criteria, Pageable pageable) {
        JobOffer offer = jobOfferRepository.findById(jobOfferId).orElse(null);
        if (offer == null) return Optional.empty();

        if (resolveReviewerForCompany(reviewerEmail, offer.getCompany().getId()).isEmpty()) {
            return Optional.empty();
        }

        Specification<JobApplication> spec = JobApplicationSpecs.forOffer(offer);
        if (criteria != null) {
            if (criteria.getStatus() != null) {
                spec = spec.and(JobApplicationSpecs.hasStatus(criteria.getStatus()));
            }
            if (criteria.getReviewed() != null) {
                spec = spec.and(JobApplicationSpecs.isReviewed(criteria.getReviewed()));
            }
            if (criteria.getMinRating() != null) {
                spec = spec.and(JobApplicationSpecs.minRating(criteria.getMinRating()));
            }
        }
        return Optional.of(jobApplicationRepository.findAll(spec, pageable).map(JobApplicationDTO::from));
    }

    // Reviewing requires the caller to be an OWNER/RECRUITER of the SAME company that
    // posted the offer this application belongs to — the check the old updateJobApplication
    // was missing (it let any COMPANYUSER review any application, regardless of company).
    // Fields in the DTO are all nullable: null means "leave unchanged", letting the caller
    // update just a status, just a note, etc. reviewedAt/reviewedBy are set server-side.
    @Transactional
    public Optional<JobApplicationDTO> review(String reviewerEmail, Long applicationId, ReviewApplicationDTO dto) {
        JobApplication application = jobApplicationRepository.findById(applicationId).orElse(null);
        if (application == null) return Optional.empty();

        Long postingCompanyId = application.getJobOffer().getCompany().getId();
        CompanyUserProfile reviewer = resolveReviewerForCompany(reviewerEmail, postingCompanyId).orElse(null);
        if (reviewer == null) return Optional.empty();

        if (dto.getApplicationStatus() != null) application.setApplicationStatus(dto.getApplicationStatus());
        if (dto.getCompanyUserNote() != null) application.setCompanyUserNote(dto.getCompanyUserNote());
        if (dto.getPriority() != null) application.setPriority(dto.getPriority());
        if (dto.getRating() != null) application.setRating(dto.getRating());

        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(reviewer);

        return Optional.of(JobApplicationDTO.from(jobApplicationRepository.save(application)));
    }

    // Visible only to the applicant themselves or the posting company's own
    // OWNER/RECRUITER — the old GET /{id} let any authenticated user (any role)
    // read any application's cover letter/notes by guessing an id. "Doesn't exist"
    // and "exists but not yours" both return empty -> 404, so the controller never
    // confirms whether an application id you can't access exists.
    @Transactional(readOnly = true)
    public Optional<JobApplicationDTO> getApplicationById(String callerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId).orElse(null);
        if (application == null) return Optional.empty();

        User user = userRepository.findByEmail(callerEmail).orElse(null);
        if (user == null) return Optional.empty();

        boolean isApplicant = candidateProfileRepository.findByUser(user)
                .map(applicant -> applicant.getId().equals(application.getApplicant().getId()))
                .orElse(false);

        if (!isApplicant) {
            Long postingCompanyId = application.getJobOffer().getCompany().getId();
            if (resolveReviewerForCompany(callerEmail, postingCompanyId).isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(JobApplicationDTO.from(application));
    }

    // Shared authority check for both listing and reviewing: caller must be a real
    // CompanyUserProfile, OWNER or RECRUITER, AND belong to the exact company that
    // owns the offer/application in question — not just "some COMPANYUSER somewhere".
    private Optional<CompanyUserProfile> resolveReviewerForCompany(String email, Long companyId) {
        return userRepository.findByEmail(email)
                .flatMap(companyUserProfileRepository::findByUser)
                .filter(profile -> profile.getCompanyRole() == CompanyRole.OWNER || profile.getCompanyRole() == CompanyRole.RECRUITER)
                .filter(profile -> profile.getCompany().getId().equals(companyId));
    }
}
