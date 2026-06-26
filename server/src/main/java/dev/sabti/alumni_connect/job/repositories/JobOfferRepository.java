package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

// JpaSpecificationExecutor backs the dynamic offers search; the derived findBy* are fixed lookups.
@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
    Page<JobOffer> findByCompany(Company company, Pageable pageable);

    // Company dashboard stat counts. COALESCE so a company with no offers sums to 0, not null.
    long countByCompany(Company company);
    long countByCompanyAndStatus(Company company, JobStatus status);

    @Query("SELECT COALESCE(SUM(o.currentApplicationCount), 0) FROM JobOffer o WHERE o.company = :company")
    long sumApplicationCountByCompany(@Param("company") Company company);

    // OPEN offers sharing >=1 skill, ranked by overlap then newest; a dedicated query since it orders by an aggregate.
    @Query(value = "SELECT o FROM JobOffer o JOIN o.skillsRequired s "
            + "WHERE o.status = dev.sabti.alumni_connect.job.entities.JobStatus.OPEN AND lower(s) IN :skills "
            + "GROUP BY o ORDER BY COUNT(s) DESC, o.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM JobOffer o JOIN o.skillsRequired s "
            + "WHERE o.status = dev.sabti.alumni_connect.job.entities.JobStatus.OPEN AND lower(s) IN :skills")
    Page<JobOffer> findOpenOffersMatchingSkills(@Param("skills") Collection<String> skills, Pageable pageable);
}
