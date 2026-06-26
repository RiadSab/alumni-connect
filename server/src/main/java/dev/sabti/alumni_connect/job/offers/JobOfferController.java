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

    // Public, optionally-filtered browse of OPEN offers; default sort is newest-first.
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

    // 403 if the caller isn't a company OWNER/RECRUITER, or if their company isn't ACTIVE.
    @PostMapping
    public ResponseEntity<JobOfferDTO> postJobOffer(@AuthenticationPrincipal UserDetails principal,
                                                     @RequestBody @Valid CreateJobOfferDTO dto) {
        JobOfferDTO offer = jobOfferService.postJobOffer(principal.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(offer);
    }

    // The caller's own company's offers, any status; must precede /{id} so "me" isn't parsed as an id.
    @GetMapping("/me")
    public Page<JobOfferDTO> getMyCompanyJobOffers(@AuthenticationPrincipal UserDetails principal,
                                                   @PageableDefault Pageable pageable) {
        return jobOfferService.getMyCompanyJobOffers(principal.getUsername(), pageable);
    }

    // Company dashboard counts — the aggregated companion to /me.
    @GetMapping("/me/stats")
    public CompanyOfferStatsDTO getMyCompanyStats(@AuthenticationPrincipal UserDetails principal) {
        return jobOfferService.getMyCompanyStats(principal.getUsername());
    }

    // CANDIDATE-only — OPEN offers ranked by skill overlap; literal path so it precedes /{id}.
    @GetMapping("/recommended")
    public ResponseEntity<Page<JobOfferDTO>> getRecommendedOffers(@AuthenticationPrincipal UserDetails principal,
                                                                   @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(jobOfferService.getRecommendedOffers(principal.getUsername(), pageable));
    }

    // OPEN is public (principal may be null); non-OPEN only to the poster or an applicant. 404 hides existence.
    @GetMapping("/{id}")
    public JobOfferDTO getJobOfferById(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        String email = principal != null ? principal.getUsername() : null;
        return jobOfferService.getJobOfferById(email, id);
    }

    // Partial update; status is editable, so closing/reopening is just PATCH status. 404 if not the caller's offer.
    @PatchMapping("/{id}")
    public JobOfferDTO updateJobOffer(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails principal,
                                      @RequestBody @Valid UpdateJobOfferDTO dto) {
        return jobOfferService.updateJobOffer(principal.getUsername(), id, dto);
    }

    // Posting company's OWNER/RECRUITER only (404 hides existence); optional status/reviewed/minRating triage filters.
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

    // Apply (multipart): optional resume PDF upload or useProfileResume to copy the profile CV; upload wins; non-PDF is 400.
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
