package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyUserProfileRepository companyUserProfileRepository;
    @InjectMocks private CompanyUserService service;

    private static final String ACTOR_EMAIL = "owner@example.com";
    private static final long ACTOR_USER_ID = 1L;
    private static final long COMPANY_ID = 10L;

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("First");
        u.setLastName("Last");
        return u;
    }

    private Company company(long id) {
        Company c = new Company();
        c.setId(id);
        c.setName("Acme");
        return c;
    }

    private CompanyUserProfile profile(User user, Company company, CompanyRole role) {
        CompanyUserProfile p = new CompanyUserProfile();
        p.setUser(user);
        p.setCompany(company);
        p.setCompanyRole(role);
        return p;
    }

    // Wires the actor to resolve to an OWNER of COMPANY_ID.
    private User wireOwnerActor() {
        User actor = user(ACTOR_USER_ID);
        when(userRepository.findByEmail(ACTOR_EMAIL)).thenReturn(Optional.of(actor));
        when(companyUserProfileRepository.findByUser(actor))
                .thenReturn(Optional.of(profile(actor, company(COMPANY_ID), CompanyRole.OWNER)));
        return actor;
    }

    // --- getMyProfile() / requireUser+requireCompanyUser ---------------------

    @Test
    void getMyProfile_callerIsNotACompanyUser_throwsForbidden() {
        User user = user(2L);
        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile("x@example.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not a company user");
    }

    // --- getProfileById() ----------------------------------------------------

    @Test
    void getProfileById_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company user not found");
    }

    @Test
    void getProfileById_userIsNotACompanyUser_throwsNotFound() {
        User user = user(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        when(companyUserProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company user not found");
    }

    // --- changeMemberRole() --------------------------------------------------

    @Test
    void changeMemberRole_actorIsNotOwner_throwsForbidden() {
        User actor = user(ACTOR_USER_ID);
        when(userRepository.findByEmail(ACTOR_EMAIL)).thenReturn(Optional.of(actor));
        when(companyUserProfileRepository.findByUser(actor))
                .thenReturn(Optional.of(profile(actor, company(COMPANY_ID), CompanyRole.RECRUITER)));

        assertThatThrownBy(() -> service.changeMemberRole(ACTOR_EMAIL, 2L, CompanyRole.MEMBER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the company owner can change member roles");
    }

    @Test
    void changeMemberRole_grantingOwner_throwsForbidden() {
        wireOwnerActor();

        assertThatThrownBy(() -> service.changeMemberRole(ACTOR_EMAIL, 2L, CompanyRole.OWNER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Granting the OWNER role is not supported here");
    }

    @Test
    void changeMemberRole_changingOwnRole_throwsForbidden() {
        wireOwnerActor();

        assertThatThrownBy(() -> service.changeMemberRole(ACTOR_EMAIL, ACTOR_USER_ID, CompanyRole.MEMBER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You cannot change your own role");
    }

    @Test
    void changeMemberRole_targetNotFound_throwsNotFound() {
        wireOwnerActor();
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeMemberRole(ACTOR_EMAIL, 2L, CompanyRole.RECRUITER))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Member not found");
    }

    @Test
    void changeMemberRole_targetInAnotherCompany_throwsNotFound_soItIsNotProbeable() {
        wireOwnerActor();
        User target = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(companyUserProfileRepository.findByUser(target))
                .thenReturn(Optional.of(profile(target, company(COMPANY_ID + 1), CompanyRole.MEMBER)));

        assertThatThrownBy(() -> service.changeMemberRole(ACTOR_EMAIL, 2L, CompanyRole.RECRUITER))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Member not found");
    }

    @Test
    void changeMemberRole_ownerPromotesMemberInSameCompany_setsRoleAndSaves() {
        wireOwnerActor();
        User target = user(2L);
        CompanyUserProfile targetProfile = profile(target, company(COMPANY_ID), CompanyRole.MEMBER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(companyUserProfileRepository.findByUser(target)).thenReturn(Optional.of(targetProfile));

        CompanyUserProfileDTO result = service.changeMemberRole(ACTOR_EMAIL, 2L, CompanyRole.RECRUITER);

        assertThat(targetProfile.getCompanyRole()).isEqualTo(CompanyRole.RECRUITER);
        assertThat(result.getCompanyRole()).isEqualTo(CompanyRole.RECRUITER);
        verify(companyUserProfileRepository).save(targetProfile);
    }
}
