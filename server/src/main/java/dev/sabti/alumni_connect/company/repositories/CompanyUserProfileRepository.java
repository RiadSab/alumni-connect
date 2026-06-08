package dev.sabti.alumni_connect.company.repositories;

import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyUserProfileRepository extends JpaRepository<CompanyUserProfile, Long> {
}
