package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.InterviewMode;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.Priority;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobApplicationRepository;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFile;
import dev.sabti.alumni_connect.storage.StoredFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit tests for the application logic, with all repositories mocked (no database). Two themes:
//   - apply()/withdraw() branch behaviour (each failure throws its own typed exception);
//   - the id-probing guarantee: "not yours" must be reported as 404, identical to "doesn't exist".
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private CompanyUserProfileRepository companyUserProfileRepository;
    @Mock private StoredFileService storedFileService;
    @Mock private EmailSender emailSender;
    @InjectMocks private JobApplicationService service;

    // Both are @Value fields, so Mockito leaves them null.
    @BeforeEach
    void injectConfiguredValues() {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://alumni.example.com");
        ReflectionTestUtils.setField(service, "timezone", "Africa/Casablanca");
    }

    private static final String CANDIDATE_EMAIL = "candidate@example.com";
    private static final String REVIEWER_EMAIL = "reviewer@example.com";
    private static final long COMPANY_ID = 10L;

    // --- builders ------------------------------------------------------------

    private User user(long id, String first, String last) {
        User u = new User();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        return u;
    }

    private CandidateProfile candidate(long id, User u) {
        CandidateProfile c = new CandidateProfile();
        c.setId(id);
        c.setUser(u);
        return c;
    }

    private Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private CompanyUserProfile reviewer(long id, User u, Company company, CompanyRole role) {
        CompanyUserProfile p = new CompanyUserProfile();
        p.setId(id);
        p.setUser(u);
        p.setCompany(company);
        p.setCompanyRole(role);
        return p;
    }

    private JobOffer offer(long id, Company company, JobStatus status) {
        JobOffer o = new JobOffer();
        o.setId(id);
        o.setTitle("Backend Engineer");
        o.setCompany(company);
        o.setStatus(status);
        return o;
    }

    private JobApplication application(long id, JobOffer offer, CandidateProfile applicant, ApplicationStatus status) {
        JobApplication a = new JobApplication();
        a.setId(id);
        a.setJobOffer(offer);
        a.setApplicant(applicant);
        a.setApplicationStatus(status);
        return a;
    }

    // Wires userRepository + candidateProfileRepository so the caller resolves to this candidate.
    private CandidateProfile asCandidate(String email, long candidateId) {
        User u = user(candidateId * 10, "Cand", "Idate");
        CandidateProfile c = candidate(candidateId, u);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(u));
        when(candidateProfileRepository.findByUser(u)).thenReturn(Optional.of(c));
        return c;
    }

    // Wires the caller to resolve to an OWNER/RECRUITER of the given company.
    private CompanyUserProfile asReviewer(String email, long reviewerId, Company company, CompanyRole role) {
        User u = user(reviewerId * 10, "Rev", "Iewer");
        CompanyUserProfile p = reviewer(reviewerId, u, company, role);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(u));
        when(companyUserProfileRepository.findByUser(u)).thenReturn(Optional.of(p));
        return p;
    }

    // --- apply() -------------------------------------------------------------

    @Test
    void apply_notACandidate_throwsForbidden() {
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only candidates can apply");
    }

    @Test
    void apply_offerNotFound_throwsNotFound() {
        asCandidate(CANDIDATE_EMAIL, 1L);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    @Test
    void apply_offerNotOpen_throwsConflict() {
        asCandidate(CANDIDATE_EMAIL, 1L);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer(1L, company(COMPANY_ID), JobStatus.CLOSED)));

        assertThatThrownBy(() -> service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This offer is no longer open");
    }

    @Test
    void apply_alreadyHasActiveApplication_throwsConflict() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You have already applied to this offer");
    }

    @Test
    void apply_capReached_throwsConflict() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        offer.setMaxApplications(2);
        offer.setCurrentApplicationCount(2);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)).thenReturn(false);

        assertThatThrownBy(() -> service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This offer is no longer accepting applications");
    }

    @Test
    void apply_useProfileResumeButNoneOnProfile_throwsBadRequest() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        applicant.setResumeId(null);
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)).thenReturn(false);

        assertThatThrownBy(() -> service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No profile resume to reuse");
    }

    @Test
    void apply_uploadedResume_storesItIncrementsCountAndReturnsDto() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)).thenReturn(false);

        MultipartFile resume = mock(MultipartFile.class);
        when(resume.isEmpty()).thenReturn(false);
        when(storedFileService.store(resume)).thenReturn(storedFile("stored-1"));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        JobApplicationDTO result = service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), resume, null);

        assertThat(result.getResumeStorageId()).isEqualTo("stored-1");
        assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(offer.getCurrentApplicationCount()).isEqualTo(1);
        verify(storedFileService, never()).copy(anyString());
    }

    @Test
    void apply_useProfileResume_copiesTheProfileResume() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        applicant.setResumeId("profile-resume");
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)).thenReturn(false);
        when(storedFileService.copy("profile-resume")).thenReturn(storedFile("copy-1"));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        JobApplicationDTO result = service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), null, true);

        assertThat(result.getResumeStorageId()).isEqualTo("copy-1");
        verify(storedFileService, never()).store(any());
    }

    @Test
    void apply_uploadedResumeWinsOverProfileResume() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        applicant.setResumeId("profile-resume");
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobApplicationRepository.existsByJobOfferAndApplicantAndApplicationStatusNot(
                offer, applicant, ApplicationStatus.WITHDRAWN)).thenReturn(false);

        MultipartFile resume = mock(MultipartFile.class);
        when(resume.isEmpty()).thenReturn(false);
        when(storedFileService.store(resume)).thenReturn(storedFile("uploaded"));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        JobApplicationDTO result = service.apply(CANDIDATE_EMAIL, 1L, new ApplyToJobOfferDTO(), resume, true);

        assertThat(result.getResumeStorageId()).isEqualTo("uploaded");
        verify(storedFileService, never()).copy(anyString());
    }

    // --- withdraw() ----------------------------------------------------------

    @Test
    void withdraw_applicationNotFound_throwsNotFound() {
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(CANDIDATE_EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void withdraw_notTheApplicant_throwsNotFound_soItIsNotProbeable() {
        // The application belongs to candidate 1, but candidate 2 is asking to withdraw it.
        CandidateProfile owner = candidate(1L, user(11L, "Owner", "One"));
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), owner, ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        asCandidate(CANDIDATE_EMAIL, 2L); // a different candidate

        assertThatThrownBy(() -> service.withdraw(CANDIDATE_EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void withdraw_alreadyWithdrawn_isIdempotentAndChangesNothing() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        offer.setCurrentApplicationCount(3);
        JobApplication application = application(5L, offer, applicant, ApplicationStatus.WITHDRAWN);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));

        JobApplicationDTO result = service.withdraw(CANDIDATE_EMAIL, 5L);

        assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(offer.getCurrentApplicationCount()).isEqualTo(3); // unchanged
        verify(jobApplicationRepository, never()).save(any());
        verify(jobOfferRepository, never()).save(any());
    }

    @Test
    void withdraw_active_setsWithdrawnAndDecrementsCount() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobOffer offer = offer(1L, company(COMPANY_ID), JobStatus.OPEN);
        offer.setCurrentApplicationCount(3);
        JobApplication application = application(5L, offer, applicant, ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));

        JobApplicationDTO result = service.withdraw(CANDIDATE_EMAIL, 5L);

        assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(application.getApplicationStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(offer.getCurrentApplicationCount()).isEqualTo(2);
    }

    // --- getApplicationById() ------------------------------------------------

    @Test
    void getApplicationById_notFound_throwsNotFound() {
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicationById(CANDIDATE_EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void getApplicationById_notApplicantNorReviewer_throwsNotFound_soItIsNotProbeable() {
        CandidateProfile owner = candidate(1L, user(11L, "Owner", "One"));
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), owner, ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        // Caller resolves to nobody: not the applicant, not a company reviewer.
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicationById(CANDIDATE_EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void getApplicationById_theApplicant_canRead() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), applicant, ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));

        JobApplicationDTO result = service.getApplicationById(CANDIDATE_EMAIL, 5L);

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void getApplicationById_theApplicant_doesNotSeeThePrivateReviewFields() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), applicant, ApplicationStatus.APPLIED);
        application.setCompanyUserNote("weak on system design");
        application.setPriority(Priority.LOW);
        application.setRating(3);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));

        JobApplicationDTO result = service.getApplicationById(CANDIDATE_EMAIL, 5L);

        assertThat(result.getCompanyUserNote()).isNull();
        assertThat(result.getPriority()).isNull();
        assertThat(result.getRating()).isNull();
    }

    @Test
    void getApplicationById_theReviewer_stillSeesThePrivateReviewFields() {
        Company company = company(COMPANY_ID);
        JobApplication application = application(5L, offer(1L, company, JobStatus.OPEN),
                candidate(1L, user(11L, "A", "B")), ApplicationStatus.APPLIED);
        application.setCompanyUserNote("weak on system design");
        application.setRating(3);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.OWNER);

        JobApplicationDTO result = service.getApplicationById(REVIEWER_EMAIL, 5L);

        assertThat(result.getCompanyUserNote()).isEqualTo("weak on system design");
        assertThat(result.getRating()).isEqualTo(3);
    }

    // --- getApplicationResume() ----------------------------------------------

    @Test
    void getApplicationResume_accessibleButNoResume_throwsNotFound() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), applicant, ApplicationStatus.APPLIED);
        application.setResumeStorageId(null);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.getApplicationResume(CANDIDATE_EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application resume not found");
    }

    @Test
    void getApplicationResume_accessibleWithResume_returnsTheDownload() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), applicant, ApplicationStatus.APPLIED);
        application.setResumeStorageId("resume-1");
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        FileDownload download = new FileDownload(storedFile("resume-1"), mock(Resource.class));
        when(storedFileService.load("resume-1")).thenReturn(Optional.of(download));

        FileDownload result = service.getApplicationResume(CANDIDATE_EMAIL, 5L);

        assertThat(result).isSameAs(download);
    }

    // --- getApplicationsForOffer() -------------------------------------------

    @Test
    void getApplicationsForOffer_offerNotFound_throwsNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicationsForOffer(REVIEWER_EMAIL, 1L, null, PageRequest.of(0, 10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    @Test
    void getApplicationsForOffer_notAReviewerOfThatCompany_throwsNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer(1L, company(COMPANY_ID), JobStatus.OPEN)));
        when(userRepository.findByEmail(REVIEWER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicationsForOffer(REVIEWER_EMAIL, 1L, null, PageRequest.of(0, 10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    @Test
    void getApplicationsForOffer_reviewer_getsTheApplicants() {
        Company company = company(COMPANY_ID);
        JobOffer offer = offer(1L, company, JobStatus.OPEN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.RECRUITER);
        JobApplication application = application(5L, offer, candidate(1L, user(11L, "A", "B")), ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application)));

        Page<JobApplicationDTO> result = service.getApplicationsForOffer(REVIEWER_EMAIL, 1L, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // --- review() ------------------------------------------------------------

    @Test
    void review_applicationNotFound_throwsNotFound() {
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(REVIEWER_EMAIL, 5L, new ReviewApplicationDTO()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void review_notAReviewerOfThePostingCompany_throwsNotFound() {
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN),
                candidate(1L, user(11L, "A", "B")), ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(REVIEWER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(REVIEWER_EMAIL, 5L, new ReviewApplicationDTO()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void review_reviewer_appliesOnlyProvidedFieldsAndStampsReviewer() {
        Company company = company(COMPANY_ID);
        JobApplication application = application(5L, offer(1L, company, JobStatus.OPEN),
                candidate(1L, user(11L, "A", "B")), ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        CompanyUserProfile reviewer = asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.OWNER);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationDTO dto = new ReviewApplicationDTO();
        dto.setApplicationStatus(ApplicationStatus.ACCEPTED);
        dto.setCompanyUserNote("strong candidate");
        dto.setRating(8);
        // priority left null -> must stay unchanged

        JobApplicationDTO result = service.review(REVIEWER_EMAIL, 5L, dto);

        assertThat(application.getApplicationStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(application.getCompanyUserNote()).isEqualTo("strong candidate");
        assertThat(application.getRating()).isEqualTo(8);
        assertThat(application.getPriority()).isNull();
        assertThat(application.getReviewedAt()).isNotNull();
        assertThat(application.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedById()).isEqualTo(9L);
    }

    @Test
    void review_statusChanged_emailsTheApplicant() {
        Company company = company(COMPANY_ID);
        company.setName("Acme");
        User applicantUser = user(11L, "A", "B");
        applicantUser.setEmail("applicant@example.com");
        JobApplication application = application(5L, offer(1L, company, JobStatus.OPEN),
                candidate(1L, applicantUser), ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.OWNER);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationDTO dto = new ReviewApplicationDTO();
        dto.setApplicationStatus(ApplicationStatus.ACCEPTED);

        service.review(REVIEWER_EMAIL, 5L, dto);

        verify(emailSender).send(eq("applicant@example.com"),
                contains("Backend Engineer"), contains("accepted"));
    }

    @Test
    void review_statusUnchanged_doesNotEmailTheApplicant() {
        Company company = company(COMPANY_ID);
        JobApplication application = application(5L, offer(1L, company, JobStatus.OPEN),
                candidate(1L, user(11L, "A", "B")), ApplicationStatus.UNDER_REVIEW);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.OWNER);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationDTO dto = new ReviewApplicationDTO();
        dto.setCompanyUserNote("internal note");
        dto.setRating(7);

        service.review(REVIEWER_EMAIL, 5L, dto);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void review_internalStatusChange_doesNotEmailTheApplicant() {
        Company company = company(COMPANY_ID);
        JobApplication application = application(5L, offer(1L, company, JobStatus.OPEN),
                candidate(1L, user(11L, "A", "B")), ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.OWNER);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationDTO dto = new ReviewApplicationDTO();
        dto.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);

        service.review(REVIEWER_EMAIL, 5L, dto);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // --- review(): interview details -----------------------------------------

    // Reviewer of the posting company + an application ready to be scheduled.
    private JobApplication readyToSchedule(ApplicationStatus status) {
        Company company = company(COMPANY_ID);
        company.setName("Acme");
        User applicantUser = user(11L, "A", "B");
        applicantUser.setEmail("applicant@example.com");
        JobApplication application = application(5L, offer(1L, company, JobStatus.OPEN),
                candidate(1L, applicantUser), status);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        asReviewer(REVIEWER_EMAIL, 9L, company, CompanyRole.OWNER);
        return application;
    }

    private ReviewApplicationDTO scheduleOnline(String link) {
        ReviewApplicationDTO dto = new ReviewApplicationDTO();
        dto.setApplicationStatus(ApplicationStatus.SCHEDULED_INTERVIEW);
        dto.setInterviewMode(InterviewMode.ONLINE);
        dto.setInterviewAt(OffsetDateTime.now().plusDays(3));
        dto.setInterviewerName("Jane Doe");
        dto.setInterviewLink(link);
        return dto;
    }

    @Test
    void review_scheduleWithoutDetails_throwsBadRequest() {
        readyToSchedule(ApplicationStatus.APPLIED);

        ReviewApplicationDTO dto = new ReviewApplicationDTO();
        dto.setApplicationStatus(ApplicationStatus.SCHEDULED_INTERVIEW);

        assertThatThrownBy(() -> service.review(REVIEWER_EMAIL, 5L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Interview mode is required");
    }

    @Test
    void review_scheduleInThePast_throwsBadRequest() {
        readyToSchedule(ApplicationStatus.APPLIED);

        ReviewApplicationDTO dto = scheduleOnline("https://meet.example.com/abc");
        dto.setInterviewAt(OffsetDateTime.now().minusHours(1));

        assertThatThrownBy(() -> service.review(REVIEWER_EMAIL, 5L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The interview must be scheduled in the future");
    }

    @Test
    void review_scheduleOnlineWithoutAnHttpsLink_throwsBadRequest() {
        readyToSchedule(ApplicationStatus.APPLIED);

        assertThatThrownBy(() -> service.review(REVIEWER_EMAIL, 5L, scheduleOnline("javascript:alert(1)")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An https meeting link is required for an online interview");
    }

    @Test
    void review_scheduleOnsiteWithoutALocation_throwsBadRequest() {
        readyToSchedule(ApplicationStatus.APPLIED);

        ReviewApplicationDTO dto = scheduleOnline(null);
        dto.setInterviewMode(InterviewMode.ONSITE);

        assertThatThrownBy(() -> service.review(REVIEWER_EMAIL, 5L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A location is required for an on-site interview");
    }

    @Test
    void review_scheduleOnline_storesTheDetailsAndEmailsThem() {
        JobApplication application = readyToSchedule(ApplicationStatus.UNDER_REVIEW);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        service.review(REVIEWER_EMAIL, 5L, scheduleOnline("https://meet.example.com/abc"));

        assertThat(application.getInterviewMode()).isEqualTo(InterviewMode.ONLINE);
        assertThat(application.getInterviewLink()).isEqualTo("https://meet.example.com/abc");
        assertThat(application.getInterviewerName()).isEqualTo("Jane Doe");
        verify(emailSender).send(eq("applicant@example.com"),
                contains("Interview scheduled"), contains("https://meet.example.com/abc"));
    }

    @Test
    void review_switchToOnsite_dropsTheStaleMeetingLink() {
        JobApplication application = readyToSchedule(ApplicationStatus.SCHEDULED_INTERVIEW);
        application.setInterviewMode(InterviewMode.ONLINE);
        application.setInterviewLink("https://meet.example.com/old");
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationDTO dto = scheduleOnline(null);
        dto.setInterviewMode(InterviewMode.ONSITE);
        dto.setInterviewLocation("12 Rue des Fleurs, Casablanca");

        service.review(REVIEWER_EMAIL, 5L, dto);

        assertThat(application.getInterviewLink()).isNull();
        assertThat(application.getInterviewLocation()).isEqualTo("12 Rue des Fleurs, Casablanca");
    }

    @Test
    void review_rescheduleAnAlreadyScheduledInterview_stillEmailsTheApplicant() {
        readyToSchedule(ApplicationStatus.SCHEDULED_INTERVIEW);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        service.review(REVIEWER_EMAIL, 5L, scheduleOnline("https://meet.example.com/new"));

        verify(emailSender).send(anyString(), anyString(), contains("https://meet.example.com/new"));
    }

    // --- getApplicantProfile() -----------------------------------------------

    @Test
    void getApplicantProfile_notAReviewerOfThePostingCompany_throwsNotFound() {
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN),
                candidate(1L, user(11L, "A", "B")), ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(REVIEWER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicantProfile(REVIEWER_EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");
    }

    // --- getMyApplications() -------------------------------------------------

    @Test
    void getMyApplications_notACandidate_throwsForbidden() {
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyApplications(CANDIDATE_EMAIL, null, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not a candidate");
    }

    @Test
    void getMyApplications_candidate_returnsTheirApplications() {
        CandidateProfile applicant = asCandidate(CANDIDATE_EMAIL, 1L);
        JobApplication application = application(5L, offer(1L, company(COMPANY_ID), JobStatus.OPEN), applicant, ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findByApplicant(applicant, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(application)));

        Page<JobApplicationDTO> result = service.getMyApplications(CANDIDATE_EMAIL, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private StoredFile storedFile(String storageId) {
        StoredFile f = new StoredFile();
        f.setStorageId(storageId);
        return f;
    }
}
