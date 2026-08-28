package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.report.EmploymentReportDTO;
import dev.sabti.alumni_connect.report.ReportRows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlumniRecordRepository extends JpaRepository<AlumniRecord, Long> {
    Optional<AlumniRecord> findByStudentId(String studentId);

    List<AlumniRecord> findByStudentIdIn(List<String> studentIds);

    Page<AlumniRecord> findByPromotionYear(Integer promotionYear, Pageable pageable);

    // Invite targets: not claimed, not opted out, and the school has an address for them.
    List<AlumniRecord> findByClaimedByIsNullAndOptedOutAtIsNullAndEmailIsNotNull();

    List<AlumniRecord> findByPromotionYearAndClaimedByIsNullAndOptedOutAtIsNullAndEmailIsNotNull(Integer promotionYear);

    boolean existsByClaimedBy(User user);

    Optional<AlumniRecord> findByEmailIgnoreCaseAndClaimedByIsNullAndOptedOutAtIsNull(String email);

    long countByPromotionYear(Integer promotionYear);

    long countByPromotionYearAndClaimedByIsNotNull(Integer promotionYear);

    @Query("select distinct r.promotionYear from AlumniRecord r order by r.promotionYear desc")
    List<Integer> findPromotionYears();

    // Responded = claimed the account AND told us something about their situation.
    @Query("""
            select count(distinct r.id) from AlumniRecord r
            where r.promotionYear = :promotionYear
              and r.claimedBy is not null
              and exists (select 1 from EmploymentEntry e where e.candidateProfile.user = r.claimedBy)
            """)
    long countResponded(@Param("promotionYear") Integer promotionYear);

    // Current situation per person: only open periods count as "now".
    @Query("""
            select new dev.sabti.alumni_connect.report.ReportRows$StatusRow(r.claimedBy.id, e.status)
            from AlumniRecord r
            join EmploymentEntry e on e.candidateProfile.user = r.claimedBy
            where r.promotionYear = :promotionYear and e.endedAt is null
            """)
    List<ReportRows.StatusRow> findCurrentStatuses(@Param("promotionYear") Integer promotionYear);

    @Query("""
            select new dev.sabti.alumni_connect.report.ReportRows$FirstJobRow(r.claimedBy.id, min(e.startedAt))
            from AlumniRecord r
            join EmploymentEntry e on e.candidateProfile.user = r.claimedBy
            where r.promotionYear = :promotionYear and e.status = dev.sabti.alumni_connect.employment.EmploymentStatus.EMPLOYED
            group by r.claimedBy.id
            """)
    List<ReportRows.FirstJobRow> findFirstJobDates(@Param("promotionYear") Integer promotionYear);

    @Query("""
            select new dev.sabti.alumni_connect.report.EmploymentReportDTO$EmployerCount(e.employer, count(distinct r.claimedBy.id))
            from AlumniRecord r
            join EmploymentEntry e on e.candidateProfile.user = r.claimedBy
            where r.promotionYear = :promotionYear and e.employer is not null
            group by e.employer
            order by count(distinct r.claimedBy.id) desc, e.employer asc
            """)
    List<EmploymentReportDTO.EmployerCount> findTopEmployers(@Param("promotionYear") Integer promotionYear,
                                                             Pageable pageable);
}
