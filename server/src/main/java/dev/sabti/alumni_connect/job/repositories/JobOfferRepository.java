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

// JpaSpecificationExecutor backs the dynamic, optional-filter search on the public offers
// list (findAll(Specification, Pageable)) — the derived findBy* methods stay for the fixed
// single-criterion lookups (own-company offers, status).
@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
    Page<JobOffer> findByCompany(Company company, Pageable pageable);

    // Skill-based recommendations: OPEN offers that share at least one skill with the given
    // (already-lowercased) set, ranked by how many skills overlap (more matches first), newest
    // as tie-break. A Specification can filter but can't order by an aggregate, so this is a
    // dedicated query. GROUP BY o collapses the join's one-row-per-matched-skill back to one row
    // per offer; the explicit countQuery is required because the GROUP BY can't be auto-counted.
    @Query(value = "SELECT o FROM JobOffer o JOIN o.skillsRequired s "
            + "WHERE o.status = dev.sabti.alumni_connect.job.entities.JobStatus.OPEN AND lower(s) IN :skills "
            + "GROUP BY o ORDER BY COUNT(s) DESC, o.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM JobOffer o JOIN o.skillsRequired s "
            + "WHERE o.status = dev.sabti.alumni_connect.job.entities.JobStatus.OPEN AND lower(s) IN :skills")
    Page<JobOffer> findOpenOffersMatchingSkills(@Param("skills") Collection<String> skills, Pageable pageable);
}
