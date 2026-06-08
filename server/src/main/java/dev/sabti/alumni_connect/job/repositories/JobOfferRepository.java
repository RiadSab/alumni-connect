package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.job.entities.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
}
