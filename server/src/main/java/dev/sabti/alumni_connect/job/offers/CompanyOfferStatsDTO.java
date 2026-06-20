package dev.sabti.alumni_connect.job.offers;

import lombok.AllArgsConstructor;
import lombok.Data;

// Company dashboard counts, from aggregate queries instead of paging rows.
@Data
@AllArgsConstructor
public class CompanyOfferStatsDTO {
    private long totalPostings;
    private long openPostings;
    private long totalApplicants;   // sum of currentApplicationCount across the company's offers
}
