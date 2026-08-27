package dev.sabti.alumni_connect.report;

import java.util.List;

// Every rate carries its own denominator. The employment rate divides by the people who actually
// answered, never by the whole promotion: an unclaimed row is unknown, not unemployed. Divide by
// the promotion and an 80%-silent cohort reads as 15% employment, which measures the survey and
// not the graduates. See docs/employment-report.md.
public record EmploymentReportDTO(
        int promotionYear,
        long totalGraduates,
        long claimed,
        long responded,
        long employed,
        long studying,
        long seeking,
        long noCurrentPeriod,
        Double employmentRate,
        Double responseRate,
        Integer medianMonthsToFirstJob,
        List<EmployerCount> topEmployers) {

    public record EmployerCount(String employer, long count) {}
}
