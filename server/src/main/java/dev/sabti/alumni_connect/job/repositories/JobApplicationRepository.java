package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.job.entities.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
