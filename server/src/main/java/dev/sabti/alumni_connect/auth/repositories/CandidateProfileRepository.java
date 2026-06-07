package dev.sabti.alumni_connect.auth.repositories;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
}
