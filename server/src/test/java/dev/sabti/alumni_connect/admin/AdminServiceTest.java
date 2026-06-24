package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyUserProfileRepository companyUserProfileRepository;
    @InjectMocks private AdminService service;

    // --- changeUserStatus() --------------------------------------------------

    @Test
    void changeUserStatus_userNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeUserStatus(1L, "reason", UserStatus.SUSPENDED))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void changeUserStatus_alreadyInTargetStatus_throwsConflict() {
        User user = new User();
        user.setUserStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changeUserStatus(1L, "reason", UserStatus.SUSPENDED))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User is already SUSPENDED");
    }

    @Test
    void changeUserStatus_validChange_setsStatusAndReasonAndSaves() {
        User user = new User();
        user.setUserStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.changeUserStatus(1L, "policy violation", UserStatus.SUSPENDED);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getStatusChangeReason()).isEqualTo("policy violation");
        verify(userRepository).save(user);
    }

    @Test
    void changeUserStatus_activateOwnerOfPendingCompany_throwsConflict() {
        User user = new User();
        user.setUserStatus(UserStatus.PENDING);
        Company company = new Company();
        company.setStatus(CompanyStatus.PENDING);
        CompanyUserProfile profile = new CompanyUserProfile();
        profile.setUser(user);
        profile.setCompany(company);
        profile.setCompanyRole(CompanyRole.OWNER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.changeUserStatus(1L, "reason", UserStatus.ACTIVE))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Approve the company before activating its owner");
    }

    // --- changeCompanyStatus() -----------------------------------------------

    @Test
    void changeCompanyStatus_companyNotFound_throwsNotFound() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeCompanyStatus(1L, "reason", CompanyStatus.SUSPENDED))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company not found");
    }

    @Test
    void changeCompanyStatus_alreadyInTargetStatus_throwsConflict() {
        Company company = new Company();
        company.setStatus(CompanyStatus.ACTIVE);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.changeCompanyStatus(1L, "reason", CompanyStatus.ACTIVE))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Company is already ACTIVE");
    }

    @Test
    void changeCompanyStatus_validChange_setsStatusAndReasonAndSaves() {
        Company company = new Company();
        company.setStatus(CompanyStatus.PENDING);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        service.changeCompanyStatus(1L, "approved", CompanyStatus.ACTIVE);

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(company.getStatusChangeReason()).isEqualTo("approved");
        verify(companyRepository).save(company);
    }

    @Test
    void changeCompanyStatus_approve_activatesPendingOwner() {
        Company company = new Company();
        company.setStatus(CompanyStatus.PENDING);
        User owner = new User();
        owner.setUserStatus(UserStatus.PENDING);
        CompanyUserProfile ownerProfile = new CompanyUserProfile();
        ownerProfile.setUser(owner);
        ownerProfile.setCompanyRole(CompanyRole.OWNER);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyUserProfileRepository.findByCompanyAndCompanyRole(company, CompanyRole.OWNER))
                .thenReturn(List.of(ownerProfile));

        service.changeCompanyStatus(1L, "approved", CompanyStatus.ACTIVE);

        assertThat(owner.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(owner);
    }
}
