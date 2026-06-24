package dev.sabti.alumni_connect.auth.repositories;

import dev.sabti.alumni_connect.auth.entities.RefreshToken;
import dev.sabti.alumni_connect.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // The user's still-valid tokens — revoked en masse when reuse of a revoked token is detected.
    List<RefreshToken> findByUserAndRevokedAtIsNull(User user);
}
