package dev.sabti.alumni_connect.job.applications;

import lombok.Data;

// Only the cover letter is accepted; offer/applicant/status are derived server-side.
@Data
public class ApplyToJobOfferDTO {
    private String coverLetter;
}
