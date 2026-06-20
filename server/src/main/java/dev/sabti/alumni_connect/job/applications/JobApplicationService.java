package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileDTO;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobApplicationRepository;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;
    private final StoredFileService storedFileService;

    // An application is "active" until it reaches one of these.
    private static final Set<ApplicationStatus> TERMINAL_STATUSES =
            Set.of(ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);

    @Transactional
    public JobApplicationDTO apply(String applicantEmail, Long jobOfferId, ApplyToJobOfferDTO dto,
                                   MultipartFile resume, Boolean useProfileResume) {
        CandidateProfile applicant = requireCandidate(applicantEmail, "Only candidates can apply");

        JobOffer offer = jobOfferRepository.findById(jobOfferId)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));
        if (offer.getStatus() != JobStatus.OPEN) {
            throw new ConflictException("This offer is no longer open");
        }

        if (jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)) {
            // already has an active application; a withdrawn one doesn't block re-applying
            throw new ConflictException("You have already applied to this offer");
        }

        if (offer.getMaxApplications() != null
                && offer.getCurrentApplicationCount() >= offer.getMaxApplications()) {
            throw new ConflictException("This offer is no longer accepting applications");
        }

        // An uploaded offer-specific resume wins over the profile one if both are sent (it's the
        // more deliberate choice). Reusing the profile resume copies the file so this application
        // keeps its own snapshot — independent of any later profile-resume replace/delete.
        String resumeStorageId = null;
        if (resume != null && !resume.isEmpty()) {
            resumeStorageId = storedFileService.store(resume).getStorageId();
        } else if (Boolean.TRUE.equals(useProfileResume)) {
            if (applicant.getResumeId() == null) {
                throw new BadRequestException("No profile resume to reuse");
            }
            resumeStorageId = storedFileService.copy(applicant.getResumeId()).getStorageId();
        }

        JobApplication application = new JobApplication();
        application.setJobOffer(offer);
        application.setApplicant(applicant);
        application.setCoverLetter(dto.getCoverLetter());
        application.setResumeStorageId(resumeStorageId);
        application = jobApplicationRepository.save(application);

        offer.setCurrentApplicationCount(offer.getCurrentApplicationCount() + 1);
        jobOfferRepository.save(offer);

        return JobApplicationDTO.from(application);
    }

    @Transactional
    public JobApplicationDTO withdraw(String applicantEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!isApplicant(applicantEmail, application)) {
            throw new NotFoundException("Application not found");  // not yours -> 404, not probeable
        }

        if (application.getApplicationStatus() == ApplicationStatus.WITHDRAWN) {
            return JobApplicationDTO.from(application);  // idempotent: already withdrawn
        }

        application.setApplicationStatus(ApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(application);

        JobOffer offer = application.getJobOffer();
        if (offer.getCurrentApplicationCount() != null && offer.getCurrentApplicationCount() > 0) {
            offer.setCurrentApplicationCount(offer.getCurrentApplicationCount() - 1);
            jobOfferRepository.save(offer);
        }

        return JobApplicationDTO.from(application);
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationDTO> getMyApplications(String applicantEmail, Pageable pageable) {
        CandidateProfile applicant = requireCandidate(applicantEmail, "Not a candidate");
        return jobApplicationRepository.findByApplicant(applicant, pageable).map(JobApplicationDTO::from);
    }

    // Candidate dashboard counts via aggregate queries (total / active / accepted).
    @Transactional(readOnly = true)
    public MyApplicationStatsDTO getMyApplicationStats(String applicantEmail) {
        CandidateProfile applicant = requireCandidate(applicantEmail, "Not a candidate");
        long total = jobApplicationRepository.countByApplicant(applicant);
        long accepted = jobApplicationRepository.countByApplicantAndApplicationStatus(applicant, ApplicationStatus.ACCEPTED);
        long active = jobApplicationRepository.countByApplicantAndApplicationStatusNotIn(applicant, TERMINAL_STATUSES);
        return new MyApplicationStatsDTO(total, active, accepted);
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationDTO> getApplicationsForOffer(String reviewerEmail, Long jobOfferId,
                                                           JobApplicationSearchCriteria criteria, Pageable pageable) {
        JobOffer offer = jobOfferRepository.findById(jobOfferId)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));

        if (resolveReviewerForCompany(reviewerEmail, offer.getCompany().getId()).isEmpty()) {
            throw new NotFoundException("Job offer not found");  // not your offer -> 404, not probeable
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
        return jobApplicationRepository.findAll(spec, pageable).map(JobApplicationDTO::from);
    }


    @Transactional
    public JobApplicationDTO review(String reviewerEmail, Long applicationId, ReviewApplicationDTO dto) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        Long postingCompanyId = application.getJobOffer().getCompany().getId();
        CompanyUserProfile reviewer = resolveReviewerForCompany(reviewerEmail, postingCompanyId)
                .orElseThrow(() -> new NotFoundException("Application not found"));  // not your company -> 404

        if (dto.getApplicationStatus() != null) application.setApplicationStatus(dto.getApplicationStatus());
        if (dto.getCompanyUserNote() != null) application.setCompanyUserNote(dto.getCompanyUserNote());
        if (dto.getPriority() != null) application.setPriority(dto.getPriority());
        if (dto.getRating() != null) application.setRating(dto.getRating());

        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(reviewer);

        return JobApplicationDTO.from(jobApplicationRepository.save(application));
    }


    @Transactional(readOnly = true)
    public JobApplicationDTO getApplicationById(String callerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (!canAccessApplication(callerEmail, application)) {
            throw new NotFoundException("Application not found");  // not yours -> 404, not probeable
        }
        return JobApplicationDTO.from(application);
    }


    @Transactional(readOnly = true)
    public FileDownload getApplicationResume(String callerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application resume not found"));
        if (!canAccessApplication(callerEmail, application) || application.getResumeStorageId() == null) {
            throw new NotFoundException("Application resume not found");
        }

        return storedFileService.load(application.getResumeStorageId())
                .orElseThrow(() -> new NotFoundException("Application resume not found"));
    }

    @Transactional(readOnly = true)
    public CandidateProfileDTO getApplicantProfile(String reviewerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        Long postingCompanyId = application.getJobOffer().getCompany().getId();
        if (resolveReviewerForCompany(reviewerEmail, postingCompanyId).isEmpty()) {
            throw new NotFoundException("Application not found");  // not your company -> 404, not probeable
        }

        CandidateProfile applicant = application.getApplicant();
        return CandidateProfileDTO.from(applicant.getUser(), applicant);
    }

    // A candidate operation requires a real user behind the email and a CandidateProfile behind that
    // user; either missing -> 403 with the given message, since the endpoint doesn't apply to the
    // caller (e.g. a company user trying to apply or list their applications).
    private CandidateProfile requireCandidate(String email, String denyMessage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException(denyMessage));
        return candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException(denyMessage));
    }

    private boolean isApplicant(String callerEmail, JobApplication application) {
        return userRepository.findByEmail(callerEmail)
                .flatMap(candidateProfileRepository::findByUser)
                .map(applicant -> applicant.getId().equals(application.getApplicant().getId()))
                .orElse(false);
    }

    private boolean canAccessApplication(String callerEmail, JobApplication application) {
        if (isApplicant(callerEmail, application)) return true;

        Long postingCompanyId = application.getJobOffer().getCompany().getId();
        return resolveReviewerForCompany(callerEmail, postingCompanyId).isPresent();
    }

    private Optional<CompanyUserProfile> resolveReviewerForCompany(String email, Long companyId) {
        return userRepository.findByEmail(email)
                .flatMap(companyUserProfileRepository::findByUser)
                .filter(profile -> profile.getCompanyRole() == CompanyRole.OWNER || profile.getCompanyRole() == CompanyRole.RECRUITER)
                .filter(profile -> profile.getCompany().getId().equals(companyId));
    }
}
