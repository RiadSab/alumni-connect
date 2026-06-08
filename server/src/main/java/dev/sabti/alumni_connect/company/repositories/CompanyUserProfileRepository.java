package dev.sabti.alumni_connect.company.repositories;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyUserProfileRepository extends JpaRepository<CompanyUserProfile, Long> {
    Optional<CompanyUserProfile> findByUser(User user);
}
