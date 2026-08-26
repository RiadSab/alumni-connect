package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
