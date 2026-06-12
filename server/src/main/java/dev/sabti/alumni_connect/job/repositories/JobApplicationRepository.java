package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// JpaSpecificationExecutor backs the applicant-triage on an offer (filter by status/reviewed/
// rating via findAll(Specification, Pageable)); the derived methods stay for the fixed lookups.
@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    boolean existsByJobOfferAndApplicant(JobOffer jobOffer, CandidateProfile applicant);

    Page<JobApplication> findByApplicant(CandidateProfile applicant, Pageable pageable);
}
