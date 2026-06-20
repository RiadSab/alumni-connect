package dev.sabti.alumni_connect.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

// Admin dashboard counts, from aggregate queries instead of paging rows.
@Data
@AllArgsConstructor
public class AdminStatsDTO {
    private long pendingUsers;
    private long pendingCompanies;
}
