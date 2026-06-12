package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// JpaSpecificationExecutor backs the dynamic, optional-filter search on the public offers
// list (findAll(Specification, Pageable)) — the derived findBy* methods stay for the fixed
// single-criterion lookups (own-company offers, status).
@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
    Page<JobOffer> findByStatus(JobStatus status, Pageable pageable);
    Page<JobOffer> findByCompany(Company company, Pageable pageable);
}
