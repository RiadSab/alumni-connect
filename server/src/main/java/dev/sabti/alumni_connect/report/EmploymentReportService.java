package dev.sabti.alumni_connect.report;

import dev.sabti.alumni_connect.alumni.AlumniRecordRepository;
import dev.sabti.alumni_connect.employment.EmploymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmploymentReportService {
    private final AlumniRecordRepository alumniRecordRepository;

    private static final int TOP_EMPLOYERS = 5;
    // The roster carries a promotion year, not a date. Moroccan promotions graduate in the summer,
    // so July is the reference point every "months to first job" is measured from.
    private static final int GRADUATION_MONTH = 7;

    @Transactional(readOnly = true)
    public List<Integer> getPromotionYears() {
        return alumniRecordRepository.findPromotionYears();
    }

    @Transactional(readOnly = true)
    public EmploymentReportDTO getReport(int promotionYear) {
        long total = alumniRecordRepository.countByPromotionYear(promotionYear);
        long claimed = alumniRecordRepository.countByPromotionYearAndClaimedByIsNotNull(promotionYear);
        long responded = alumniRecordRepository.countResponded(promotionYear);

        Map<Long, EmploymentStatus> current = currentStatusPerPerson(promotionYear);
        long employed = countOf(current, EmploymentStatus.EMPLOYED);
        long studying = countOf(current, EmploymentStatus.STUDYING);
        long seeking = countOf(current, EmploymentStatus.SEEKING);
        // Answered once, but every period they gave us has an end date: we don't know where they are now.
        long noCurrentPeriod = Math.max(0, responded - current.size());

        return new EmploymentReportDTO(
                promotionYear,
                total,
                claimed,
                responded,
                employed,
                studying,
                seeking,
                noCurrentPeriod,
                responded == 0 ? null : (double) employed / responded,
                total == 0 ? null : (double) responded / total,
                medianMonthsToFirstJob(promotionYear),
                alumniRecordRepository.findTopEmployers(promotionYear, PageRequest.of(0, TOP_EMPLOYERS)));
    }

    private Map<Long, EmploymentStatus> currentStatusPerPerson(int promotionYear) {
        Map<Long, EmploymentStatus> current = new HashMap<>();
        for (ReportRows.StatusRow row : alumniRecordRepository.findCurrentStatuses(promotionYear)) {
            // Two open periods (a job and evening classes) count as one person, and the job wins.
            current.merge(row.userId(), row.status(),
                    (a, b) -> a == EmploymentStatus.EMPLOYED || b == EmploymentStatus.EMPLOYED
                            ? EmploymentStatus.EMPLOYED
                            : a);
        }
        return current;
    }

    private long countOf(Map<Long, EmploymentStatus> current, EmploymentStatus status) {
        return current.values().stream().filter(value -> value == status).count();
    }

    private Integer medianMonthsToFirstJob(int promotionYear) {
        LocalDate graduation = LocalDate.of(promotionYear, GRADUATION_MONTH, 1);
        List<Long> months = alumniRecordRepository.findFirstJobDates(promotionYear).stream()
                .map(row -> ChronoUnit.MONTHS.between(graduation, row.startedAt()))
                // A job that started before graduation is a zero-month wait, not a negative one.
                .map(value -> Math.max(0, value))
                .sorted()
                .toList();

        if (months.isEmpty()) return null;
        int middle = months.size() / 2;
        long median = months.size() % 2 == 1
                ? months.get(middle)
                : (months.get(middle - 1) + months.get(middle)) / 2;
        return (int) median;
    }
}
