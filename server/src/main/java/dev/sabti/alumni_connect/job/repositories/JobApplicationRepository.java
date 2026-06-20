package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

// JpaSpecificationExecutor backs the applicant-triage on an offer (filter by status/reviewed/
// rating via findAll(Specification, Pageable)); the derived methods stay for the fixed lookups.
@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    // Candidate dashboard stat counts.
    long countByApplicant(CandidateProfile applicant);
    long countByApplicantAndApplicationStatus(CandidateProfile applicant, ApplicationStatus status);
    long countByApplicantAndApplicationStatusNotIn(CandidateProfile applicant, Collection<ApplicationStatus> statuses);


    boolean existsByJobOfferAndApplicantAndApplicationStatusNot(JobOffer jobOffer, CandidateProfile applicant,
                                                                ApplicationStatus applicationStatus);

    Page<JobApplication> findByApplicant(CandidateProfile applicant, Pageable pageable);
}
