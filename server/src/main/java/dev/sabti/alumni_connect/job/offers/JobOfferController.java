package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.applications.ApplyToJobOfferDTO;
import dev.sabti.alumni_connect.job.applications.JobApplicationDTO;
import dev.sabti.alumni_connect.job.applications.JobApplicationService;
import dev.sabti.alumni_connect.job.applications.JobApplicationSearchCriteria;
import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/job-offers")
@RequiredArgsConstructor
public class JobOfferController {
    private final JobOfferService jobOfferService;
    private final JobApplicationService jobApplicationService;

    // Public, optionally-filtered browse of OPEN offers. Every filter is optional — none
    // supplied reproduces the previous unfiltered list. Default sort is newest-first; callers
    // can override with ?sort=. Bad enum values for city/employmentType yield a 400 from
    // Spring's parameter binding.
    @GetMapping
    public ResponseEntity<Page<JobOfferDTO>> getOpenJobOffers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) JobCity city,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) Boolean isRemote,
            @RequestParam(required = false) List<String> skills,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        JobOfferSearchCriteria criteria = new JobOfferSearchCriteria(q, city, employmentType, isRemote, skills);
        return ResponseEntity.ok(jobOfferService.getOpenJobOffers(criteria, pageable));
    }

    // The service throws 403 if the caller isn't a company OWNER/RECRUITER, or if their company
    // isn't ACTIVE.
    @PostMapping
    public ResponseEntity<JobOfferDTO> postJobOffer(@AuthenticationPrincipal UserDetails principal,
                                                     @RequestBody @Valid CreateJobOfferDTO dto) {
        JobOfferDTO offer = jobOfferService.postJobOffer(principal.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(offer);
    }

    // All of the caller's own company's offers, any status — the discovery entry
    // point for the same company-wide OWNER/RECRUITER authority already enforced on
    // edit/review. Must be registered (and matched in SecurityConfig) before
    // /{id} below, since "/me" would otherwise be parsed as an offer id. The service throws 403 if
    // the caller isn't a company OWNER/RECRUITER.
    @GetMapping("/me")
    public Page<JobOfferDTO> getMyCompanyJobOffers(@AuthenticationPrincipal UserDetails principal,
                                                   @PageableDefault Pageable pageable) {
        return jobOfferService.getMyCompanyJobOffers(principal.getUsername(), pageable);
    }

    // "Recommended for you" — OPEN offers ranked by skill overlap with the candidate's
    // profile. Candidate-only (gated to ROLE CANDIDATE in SecurityConfig, declared before the
    // public /** GET so it isn't made public). Literal "/recommended" so it isn't parsed as an
    // offer id by /{id} below. No default sort: the service's query supplies its own ranking.
    @GetMapping("/recommended")
    public ResponseEntity<Page<JobOfferDTO>> getRecommendedOffers(@AuthenticationPrincipal UserDetails principal,
                                                                   @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(jobOfferService.getRecommendedOffers(principal.getUsername(), pageable));
    }

    // OPEN offers are publicly visible (permitAll in SecurityConfig), so principal may be null here —
    // non-OPEN offers fall back to the posting company's own OWNER/RECRUITER, checked in the service.
    // Both "not found" and "not visible to you" throw 404, so draft postings don't leak their existence.
    @GetMapping("/{id}")
    public JobOfferDTO getJobOfferById(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        String email = principal != null ? principal.getUsername() : null;
        return jobOfferService.getJobOfferById(email, id);
    }

    // Partial update; status is one of the editable fields, so closing/reopening an offer is just
    // PATCH {"status": "CLOSED"} — no separate delete endpoint. The service throws 404 for a missing
    // offer or one that isn't the caller's company's (404-for-both, so it isn't probeable).
    @PatchMapping("/{id}")
    public JobOfferDTO updateJobOffer(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails principal,
                                      @RequestBody @Valid UpdateJobOfferDTO dto) {
        return jobOfferService.updateJobOffer(principal.getUsername(), id, dto);
    }

    // Applicant lists are private to the posting company's own OWNER/RECRUITER —
    // authority is checked in JobApplicationService, which throws 404 if the offer doesn't exist or
    // isn't the caller's company's (404-for-both, not probeable). Note this sub-resource
    // must stay locked down in SecurityConfig despite the broad public GET on
    // /api/job-offers/**.
    // Optional triage filters (status / reviewed / minRating) let the company narrow a busy
    // offer's applicants; sort via ?sort= (default newest-first). None supplied reproduces the
    // previous unfiltered list. Authority/ordering unchanged from above.
    @GetMapping("/{id}/applications")
    public Page<JobApplicationDTO> getApplicationsForOffer(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails principal,
                                                           @RequestParam(required = false) ApplicationStatus status,
                                                           @RequestParam(required = false) Boolean reviewed,
                                                           @RequestParam(required = false) Integer minRating,
                                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        JobApplicationSearchCriteria criteria = new JobApplicationSearchCriteria(status, reviewed, minRating);
        return jobApplicationService.getApplicationsForOffer(principal.getUsername(), id, criteria, pageable);
    }

    // Named business action (apply), not a generic sub-resource POST — mirrors the
    // /approve, /reject pattern in AdminController (security/auditability rationale).
    // Multipart so the applicant can attach a resume: an offer-specific PDF upload (`resume`),
    // or `useProfileResume=true` to reuse their profile CV (copied onto the application). Both
    // optional; if both are sent the upload wins. A non-PDF upload is rejected here as 400; the
    // service throws the other reasons (403 not a candidate, 404 no such offer, 409 not open /
    // already applied / cap reached, 400 no profile resume to reuse).
    @PostMapping(value = "/{id}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobApplicationDTO> apply(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails principal,
                                                    @ModelAttribute @Valid ApplyToJobOfferDTO dto,
                                                    @RequestParam(required = false) MultipartFile resume,
                                                    @RequestParam(required = false) Boolean useProfileResume) {
        if (resume != null && !resume.isEmpty()
                && !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(resume.getContentType())) {
            throw new BadRequestException("Resume must be a PDF");
        }
        JobApplicationDTO application = jobApplicationService.apply(principal.getUsername(), id, dto, resume, useProfileResume);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }
}
