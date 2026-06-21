package dev.sabti.alumni_connect.job.repositories;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    boolean existsByCandidateAndJobOffer(CandidateProfile candidate, JobOffer jobOffer);

    void deleteByCandidateAndJobOffer(CandidateProfile candidate, JobOffer jobOffer);

    Page<SavedJob> findByCandidate(CandidateProfile candidate, Pageable pageable);

    // The candidate's saved offer ids, so the board can mark bookmarks in one query, not N.
    @Query("select s.jobOffer.id from SavedJob s where s.candidate = :candidate")
    List<Long> findSavedOfferIds(CandidateProfile candidate);
}
