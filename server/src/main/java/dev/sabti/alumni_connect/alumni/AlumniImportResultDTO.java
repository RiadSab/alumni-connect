package dev.sabti.alumni_connect.alumni;

import java.util.List;

// Outcome of one CSV upload. Valid rows are imported even when others fail, so the admin fixes
// the listed lines and re-uploads the same file — the import is keyed on student ID.
public record AlumniImportResultDTO(boolean dryRun, int created, int updated, List<RowError> errors) {
    public record RowError(long line, String message) {}
}
