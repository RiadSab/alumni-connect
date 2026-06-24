package dev.sabti.alumni_connect.company.repositories;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyUserProfileRepository extends JpaRepository<CompanyUserProfile, Long> {
    Optional<CompanyUserProfile> findByUser(User user);

    // The company roster — all members of one company, for the company-scoped listing
    // (GET /api/company-users returns the caller's own company's members).
    Page<CompanyUserProfile> findByCompany(Company company, Pageable pageable);

    // The owner's join requests — members of one company whose account is still PENDING.
    Page<CompanyUserProfile> findByCompanyAndUser_UserStatus(Company company, UserStatus status, Pageable pageable);

    // A company's profiles for a given role — used to resolve the OWNER.
    List<CompanyUserProfile> findByCompanyAndCompanyRole(Company company, CompanyRole role);
}
