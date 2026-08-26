package dev.sabti.alumni_connect.alumni;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlumniClaimTokenRepository extends JpaRepository<AlumniClaimToken, Long> {
    Optional<AlumniClaimToken> findByTokenHash(String tokenHash);

    void deleteByAlumniRecord(AlumniRecord alumniRecord);
}
