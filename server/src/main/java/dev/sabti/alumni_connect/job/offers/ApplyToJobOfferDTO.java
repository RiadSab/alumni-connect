package dev.sabti.alumni_connect.job.offers;

import lombok.Data;

// jobOffer/applicant/applicationStatus are never accepted here — the offer comes from
// the URL path, the applicant from the authenticated principal's own CandidateProfile,
// and status always starts at APPLIED. Resume/certificates are deferred until the
// file-storage feature exists (same reasoning as CreateJobOfferDTO and RegisterCandidateDTO).
@Data
public class ApplyToJobOfferDTO {
    private String coverLetter;
}
