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

    // Applying requires: a real CandidateProfile behind the caller, an OPEN offer, no active
    // (non-withdrawn) application from this candidate to this offer — a withdrawn one doesn't block
    // re-applying — and, if the offer caps applications, room left under that cap. Each failure now
    // throws its own exception with a caller-facing reason (previously all collapsed to one empty
    // 400): not a candidate -> 403, no such offer -> 404, offer not open / already applied / cap
    // reached -> 409, asked to reuse a profile resume that doesn't exist -> 400.
    // A resume is optional: the applicant either uploads one specific to this offer, or reuses
    // their profile resume (which gets copied — see below). All the "can't apply" checks run
    // before any file work, so we never store a resume for an application that won't be created.
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

    // A candidate withdraws their own application. Applicant-only: "not found" and "not yours" both
    // collapse to empty -> 404, so a candidate can't probe other people's applications. Soft, not a
    // delete: the row is kept with status WITHDRAWN, so the company sees the withdrawal (rather than
    // it vanishing mid-review) and the resume snapshot is preserved. The offer's application count is
    // decremented to free a slot under its cap. Idempotent: withdrawing an already-withdrawn
    // application returns it unchanged with no count change. After withdrawing, the candidate can
    // apply to the same offer again (apply()'s guard ignores withdrawn rows): that creates a new
    // application, leaving this withdrawn one as history.
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

    // The candidate's own application history, any status. ForbiddenException (403) if the caller has
    // no CandidateProfile (this endpoint doesn't apply to them) — old code instead used messy
    // instanceof-principal checks and silently returned an empty page for non-candidates.
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

    // Listing applicants is restricted to the OWNER/RECRUITER of the company that
    // posted THIS specific offer — same authority boundary as reviewing (see review()).
    // The optional triage filters (status / reviewed / minRating) only narrow within this
    // offer's applicants: forOffer(...) is the non-optional base, ANDed with whatever the
    // caller supplied. The authority check runs first, so filters never run for an outsider.
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

    // Reviewing requires the caller to be an OWNER/RECRUITER of the SAME company that
    // posted the offer this application belongs to — the check the old updateJobApplication
    // was missing (it let any COMPANYUSER review any application, regardless of company).
    // Fields in the DTO are all nullable: null means "leave unchanged", letting the caller
    // update just a status, just a note, etc. reviewedAt/reviewedBy are set server-side.
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

    // Visible only to the applicant themselves or the posting company's own
    // OWNER/RECRUITER — the old GET /{id} let any authenticated user (any role)
    // read any application's cover letter/notes by guessing an id. "Doesn't exist"
    // and "exists but not yours" both return empty -> 404, so the controller never
    // confirms whether an application id you can't access exists.
    @Transactional(readOnly = true)
    public JobApplicationDTO getApplicationById(String callerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (!canAccessApplication(callerEmail, application)) {
            throw new NotFoundException("Application not found");  // not yours -> 404, not probeable
        }
        return JobApplicationDTO.from(application);
    }

    // Streams the resume PDF attached to an application, behind the same access gate as
    // getApplicationById (the applicant, or the posting company's OWNER/RECRUITER). 404 for every
    // miss — application doesn't exist, caller can't reach it, or it carries no resume — all sharing
    // one message so an inaccessible id never leaks (a distinct "no resume" message would confirm the
    // application exists).
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

    // The applicant's full candidate profile, for the posting company's OWNER/RECRUITER to review
    // beyond the name shown on the applications list. Company-only (same gate as listing applicants),
    // NOT the applicant-included gate the resume download uses: the resume is a per-application
    // snapshot, but this just points at the live profile, which the applicant already reads via
    // GET /api/candidates/me. Empty -> 404 for any miss, so an application id you can't reach never leaks.
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

    // True only if the caller is the candidate who filed this application. Used by withdraw, where
    // "not the applicant" is reported as 404 (not 403) so other people's applications aren't probeable.
    private boolean isApplicant(String callerEmail, JobApplication application) {
        return userRepository.findByEmail(callerEmail)
                .flatMap(candidateProfileRepository::findByUser)
                .map(applicant -> applicant.getId().equals(application.getApplicant().getId()))
                .orElse(false);
    }

    // Shared read-access gate for a single application: the candidate who filed it, or an
    // OWNER/RECRUITER of the company that posted its offer. Used wherever "not yours" must be
    // indistinguishable from "doesn't exist" (both -> 404). Distinct from resolveReviewerForCompany,
    // which is the company-only authority used by listing/reviewing.
    private boolean canAccessApplication(String callerEmail, JobApplication application) {
        if (isApplicant(callerEmail, application)) return true;

        Long postingCompanyId = application.getJobOffer().getCompany().getId();
        return resolveReviewerForCompany(callerEmail, postingCompanyId).isPresent();
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
