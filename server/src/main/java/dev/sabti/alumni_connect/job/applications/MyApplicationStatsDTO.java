package dev.sabti.alumni_connect.job.applications;

import lombok.AllArgsConstructor;
import lombok.Data;

// Candidate dashboard counts, from aggregate queries instead of paging rows.
@Data
@AllArgsConstructor
public class MyApplicationStatsDTO {
    private long total;
    private long active;   // not yet terminal (accepted/rejected/withdrawn)
    private long accepted;
}
