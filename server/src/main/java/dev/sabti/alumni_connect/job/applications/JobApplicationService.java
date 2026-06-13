package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileDTO;
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

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;
    private final StoredFileService storedFileService;

    // Applying requires: a real CandidateProfile behind the caller, an OPEN offer,
    // no pre-existing application from this candidate to this offer, and — if the
    // offer caps applications — room left under that cap. All collapse to one
    // "can't apply" outcome, the same Optional "soft failure" pattern as
    // registerCompanyMember/postJobOffer (the controller maps empty -> 400).
    // A resume is optional: the applicant either uploads one specific to this offer, or reuses
    // their profile resume (which gets copied — see below). All the "can't apply" checks run
    // before any file work, so we never store a resume for an application that won't be created.
    @Transactional
    public Optional<JobApplicationDTO> apply(String applicantEmail, Long jobOfferId, ApplyToJobOfferDTO dto,
                                             MultipartFile resume, Boolean useProfileResume) {
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

        // An uploaded offer-specific resume wins over the profile one if both are sent (it's the
        // more deliberate choice). Reusing the profile resume copies the file so this application
        // keeps its own snapshot — independent of any later profile-resume replace/delete.
        String resumeStorageId = null;
        if (resume != null && !resume.isEmpty()) {
            resumeStorageId = storedFileService.store(resume).getStorageId();
        } else if (Boolean.TRUE.equals(useProfileResume)) {
            if (applicant.getResumeId() == null) return Optional.empty();  // asked to reuse, but none on file
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
        if (application == null || !canAccessApplication(callerEmail, application)) return Optional.empty();

        return Optional.of(JobApplicationDTO.from(application));
    }

    // Streams the resume PDF attached to an application, behind the same access gate as
    // getApplicationById (the applicant, or the posting company's OWNER/RECRUITER). Empty
    // for every miss — application doesn't exist, caller can't reach it, or it carries no
    // resume — all of which the controller maps to 404, so an inaccessible id never leaks.
    @Transactional(readOnly = true)
    public Optional<FileDownload> getApplicationResume(String callerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId).orElse(null);
        if (application == null || !canAccessApplication(callerEmail, application)) return Optional.empty();
        if (application.getResumeStorageId() == null) return Optional.empty();

        return storedFileService.load(application.getResumeStorageId());
    }

    // The applicant's full candidate profile, behind the same access gate as the resume download
    // (the applicant, or the posting company's OWNER/RECRUITER). Lets a company reviewer see more
    // than the name shown on the applications list. Empty -> 404 for any miss, so an application id
    // you can't reach never leaks.
    @Transactional(readOnly = true)
    public Optional<CandidateProfileDTO> getApplicantProfile(String callerEmail, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId).orElse(null);
        if (application == null || !canAccessApplication(callerEmail, application)) return Optional.empty();

        CandidateProfile applicant = application.getApplicant();
        return Optional.of(CandidateProfileDTO.from(applicant.getUser(), applicant));
    }

    // Shared read-access gate for a single application: the candidate who filed it, or an
    // OWNER/RECRUITER of the company that posted its offer. Used wherever "not yours" must be
    // indistinguishable from "doesn't exist" (both -> 404). Distinct from resolveReviewerForCompany,
    // which is the company-only authority used by listing/reviewing.
    private boolean canAccessApplication(String callerEmail, JobApplication application) {
        User user = userRepository.findByEmail(callerEmail).orElse(null);
        if (user == null) return false;

        boolean isApplicant = candidateProfileRepository.findByUser(user)
                .map(applicant -> applicant.getId().equals(application.getApplicant().getId()))
                .orElse(false);
        if (isApplicant) return true;

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
