package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByJobOfferAndApplicant(JobOffer jobOffer, CandidateProfile applicant);

    Page<JobApplication> findByJobOffer(JobOffer jobOffer, Pageable pageable);

    Page<JobApplication> findByApplicant(CandidateProfile applicant, Pageable pageable);
}
