package dev.sabti.alumni_connect.employment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmploymentConfirmTokenRepository extends JpaRepository<EmploymentConfirmToken, Long> {
    Optional<EmploymentConfirmToken> findByTokenHash(String tokenHash);
}
