package dev.sabti.alumni_connect.security;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Verifies the UserStatus -> account-flag mapping (the basis the JWT filter relies on to refuse
// suspended/disabled accounts) and the UserType -> Spring role mapping.
@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private MyUserDetailsService service;

    private static final String EMAIL = "user@example.com";

    private void givenUser(UserType type, UserStatus status) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("hash");
        user.setUserType(type);
        user.setUserStatus(status);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private UserDetails load() {
        return service.loadUserByUsername(EMAIL);
    }

    @Test
    void unknownEmail_throwsUsernameNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(this::load).isInstanceOf(UsernameNotFoundException.class);
    }

    // --- status -> flags -----------------------------------------------------

    @Test
    void activeUser_isEnabledAndUnlocked() {
        givenUser(UserType.CANDIDATE, UserStatus.ACTIVE);
        UserDetails details = load();

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void pendingUser_isEnabledAndUnlocked() {
        givenUser(UserType.CANDIDATE, UserStatus.PENDING);
        UserDetails details = load();

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void suspendedUser_isLocked() {
        givenUser(UserType.CANDIDATE, UserStatus.SUSPENDED);

        assertThat(load().isAccountNonLocked()).isFalse();
    }

    @Test
    void inactiveUser_isDisabledButNotLocked() {
        givenUser(UserType.CANDIDATE, UserStatus.INACTIVE);
        UserDetails details = load();

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void deletedUser_isDisabled() {
        givenUser(UserType.CANDIDATE, UserStatus.DELETED);

        assertThat(load().isEnabled()).isFalse();
    }

    // --- type -> role --------------------------------------------------------

    @Test
    void candidate_getsRoleCandidate() {
        givenUser(UserType.CANDIDATE, UserStatus.ACTIVE);

        assertThat(authorities()).contains("ROLE_CANDIDATE");
    }

    @Test
    void companyUser_getsRoleCompanyuser() {
        givenUser(UserType.COMPANY_USER, UserStatus.ACTIVE);

        assertThat(authorities()).contains("ROLE_COMPANYUSER");
    }

    @Test
    void administrator_getsRoleAdministrator() {
        givenUser(UserType.ADMINISTRATOR, UserStatus.ACTIVE);

        assertThat(authorities()).contains("ROLE_ADMINISTRATOR");
    }

    private java.util.List<String> authorities() {
        return load().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
