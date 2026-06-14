package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import dev.sabti.alumni_connect.storage.StoredFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyUserProfileRepository companyUserProfileRepository;
    @Mock private StoredFileService storedFileService;
    @InjectMocks private CompanyService service;

    private static final String EMAIL = "owner@example.com";

    private Company company(long id, CompanyStatus status) {
        Company c = new Company();
        c.setId(id);
        c.setName("Acme");
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

    // --- getVisibleCompanyById() ---------------------------------------------

    @Test
    void getVisibleCompanyById_notFound_throwsNotFound() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVisibleCompanyById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company not found");
    }

    @Test
    void getVisibleCompanyById_notPubliclyVisible_throwsNotFound_soHiddenCompaniesArentProbeable() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company(1L, CompanyStatus.SUSPENDED)));

        assertThatThrownBy(() -> service.getVisibleCompanyById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company not found");
    }

    @Test
    void getVisibleCompanyById_publiclyVisible_returnsDto() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company(1L, CompanyStatus.ACTIVE)));

        CompanyDTO result = service.getVisibleCompanyById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    // --- updateMyCompany() / requireOwner ------------------------------------

    @Test
    void updateMyCompany_callerHasNoCompanyProfile_throwsForbidden() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMyCompany(EMAIL, new UpdateCompanyDTO()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the company owner can do this");
    }

    @Test
    void updateMyCompany_callerIsNotOwner_throwsForbidden() {
        User user = new User();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile(user, company(1L, CompanyStatus.ACTIVE), CompanyRole.RECRUITER)));

        assertThatThrownBy(() -> service.updateMyCompany(EMAIL, new UpdateCompanyDTO()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the company owner can do this");
    }

    @Test
    void updateMyCompany_owner_appliesProvidedFields() {
        User user = new User();
        Company company = company(1L, CompanyStatus.ACTIVE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile(user, company, CompanyRole.OWNER)));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCompanyDTO dto = new UpdateCompanyDTO();
        dto.setName("New Name");

        CompanyDTO result = service.updateMyCompany(EMAIL, dto);

        assertThat(company.getName()).isEqualTo("New Name");
        assertThat(result.getName()).isEqualTo("New Name");
    }

    // --- getVisibleCompanyLogo() ---------------------------------------------

    @Test
    void getVisibleCompanyLogo_companyNotVisible_throwsNotFound() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company(1L, CompanyStatus.PENDING)));

        assertThatThrownBy(() -> service.getVisibleCompanyLogo(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company logo not found");
    }

    @Test
    void getVisibleCompanyLogo_visibleButNoLogo_throwsNotFound() {
        Company company = company(1L, CompanyStatus.ACTIVE);
        company.setLogoId(null);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.getVisibleCompanyLogo(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company logo not found");
    }
}
