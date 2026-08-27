package dev.sabti.alumni_connect.report;

import dev.sabti.alumni_connect.employment.EmploymentStatus;

import java.time.LocalDate;

// Row shapes for the aggregate queries — one per grouping the report needs.
public final class ReportRows {
    private ReportRows() {}

    public record StatusRow(Long userId, EmploymentStatus status) {}

    public record FirstJobRow(Long userId, LocalDate startedAt) {}
}
