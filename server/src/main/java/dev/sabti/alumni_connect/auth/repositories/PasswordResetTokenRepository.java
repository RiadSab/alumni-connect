package dev.sabti.alumni_connect.auth.repositories;

import dev.sabti.alumni_connect.auth.entities.PasswordResetToken;
import dev.sabti.alumni_connect.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    // The user's current unused code (issuing a new one deletes the old, so there's at most one).
    Optional<PasswordResetToken> findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}