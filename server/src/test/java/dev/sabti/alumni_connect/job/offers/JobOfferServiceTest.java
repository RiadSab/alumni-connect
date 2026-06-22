package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceTest {

    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyUserProfileRepository companyUserProfileRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @InjectMocks private JobOfferService service;

    private static final String EMAIL = "poster@example.com";
    private static final long COMPANY_ID = 10L;

    private Company company(long id, CompanyStatus status) {
        Company c = new Company();
        c.setId(id);
        c.setStatus(status);
        return c;
    }

    private CompanyUserProfile profile(User user, Company company, CompanyRole role) {
        CompanyUserProfile p = new CompanyUserProfile();
        p.setUser(user);
        p.setCompany(company);
        p.setCompanyRole(role);
        return p;
    }

    private JobOffer offer(long id, Company company, JobStatus status) {
        JobOffer o = new JobOffer();
        o.setId(id);
        o.setCompany(company);
        o.setStatus(status);
        return o;
    }

    // --- postJobOffer() ------------------------------------------------------

    @Test
    void postJobOffer_callerIsNotACompanyUser_throwsForbidden() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postJobOffer(EMAIL, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only a company owner or recruiter can post job offers");
    }

    @Test
    void postJobOffer_callerHasWrongCompanyRole_throwsForbidden() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile(user, company(COMPANY_ID, CompanyStatus.ACTIVE), CompanyRole.MEMBER)));

        assertThatThrownBy(() -> service.postJobOffer(EMAIL, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only a company owner or recruiter can post job offers");
    }

    @Test
    void postJobOffer_companyNotActive_throwsForbidden() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile(user, company(COMPANY_ID, CompanyStatus.PENDING), CompanyRole.OWNER)));

        assertThatThrownBy(() -> service.postJobOffer(EMAIL, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Your company is not active and cannot post job offers");
    }

    // --- getJobOfferById() ---------------------------------------------------

    @Test
    void getJobOfferById_notFound_throwsNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJobOfferById(EMAIL, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    @Test
    void getJobOfferById_nonOpenAndCallerIsNotAPoster_throwsNotFound_soDraftsArentLeaked() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer(1L, company(COMPANY_ID, CompanyStatus.ACTIVE), JobStatus.CLOSED)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJobOfferById(EMAIL, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    // --- updateJobOffer() ----------------------------------------------------

    @Test
    void updateJobOffer_notFound_throwsNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateJobOffer(EMAIL, 1L, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    @Test
    void updateJobOffer_callerIsNotAPosterOfThatCompany_throwsNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer(1L, company(COMPANY_ID, CompanyStatus.ACTIVE), JobStatus.OPEN)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateJobOffer(EMAIL, 1L, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job offer not found");
    }

    // --- getMyCompanyJobOffers() ---------------------------------------------

    @Test
    void getMyCompanyJobOffers_callerIsNotACompanyUser_throwsForbidden() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyCompanyJobOffers(EMAIL, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only a company user can do this");
    }

    // --- getRecommendedOffers() ----------------------------------------------

    @Test
    void getRecommendedOffers_noUser_returnsEmptyPage() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        Page<JobOfferDTO> result = service.getRecommendedOffers(EMAIL, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void getRecommendedOffers_noCandidateProfile_returnsEmptyPage() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        Page<JobOfferDTO> result = service.getRecommendedOffers(EMAIL, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void getRecommendedOffers_candidateWithNoSkills_returnsEmptyPage() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(new CandidateProfile())); // no skills

        Page<JobOfferDTO> result = service.getRecommendedOffers(EMAIL, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}
