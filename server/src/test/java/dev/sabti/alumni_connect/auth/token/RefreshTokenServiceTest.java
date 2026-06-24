package dev.sabti.alumni_connect.auth.token;

import dev.sabti.alumni_connect.auth.entities.RefreshToken;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;
    @InjectMocks private RefreshTokenService service;

    private RefreshToken validToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        return token;
    }

    @Test
    void issue_savesTokenAndReturnsRaw() {
        String raw = service.issue(new User());
        assertThat(raw).isNotBlank();
        verify(repository).save(org.mockito.ArgumentMatchers.any(RefreshToken.class));
    }

    @Test
    void rotate_validToken_revokesOldAndReturnsNew() {
        User user = new User();
        RefreshToken token = validToken(user);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        Optional<RefreshTokenService.Rotation> result = service.rotate("raw");

        assertThat(result).isPresent();
        assertThat(result.get().user()).isSameAs(user);
        assertThat(result.get().rawToken()).isNotBlank();
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void rotate_reusedToken_revokesAllAndReturnsEmpty() {
        User user = new User();
        RefreshToken token = validToken(user);
        token.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(repository.findByUserAndRevokedAtIsNull(user)).thenReturn(List.of());

        Optional<RefreshTokenService.Rotation> result = service.rotate("raw");

        assertThat(result).isEmpty();
        verify(repository).findByUserAndRevokedAtIsNull(user);
    }

    @Test
    void rotate_expiredToken_returnsEmpty() {
        RefreshToken token = validToken(new User());
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThat(service.rotate("raw")).isEmpty();
    }

    @Test
    void rotate_unknownToken_returnsEmpty() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThat(service.rotate("raw")).isEmpty();
    }

    @Test
    void revoke_setsRevokedAt() {
        RefreshToken token = validToken(new User());
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service.revoke("raw");

        assertThat(token.getRevokedAt()).isNotNull();
        verify(repository).save(token);
    }
}
