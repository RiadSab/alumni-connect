package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.applications.ApplyToJobOfferDTO;
import dev.sabti.alumni_connect.job.applications.JobApplicationDTO;
import dev.sabti.alumni_connect.job.applications.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-offers")
@RequiredArgsConstructor
public class JobOfferController {
    private final JobOfferService jobOfferService;
    private final JobApplicationService jobApplicationService;

    @GetMapping
    public ResponseEntity<Page<JobOfferDTO>> getOpenJobOffers(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(jobOfferService.getOpenJobOffers(pageable));
    }

    @PostMapping
    public ResponseEntity<JobOfferDTO> postJobOffer(@AuthenticationPrincipal UserDetails principal,
                                                     @RequestBody @Valid CreateJobOfferDTO dto) {
        return jobOfferService.postJobOffer(principal.getUsername(), dto)
                .map(offer -> ResponseEntity.status(HttpStatus.CREATED).body(offer))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Applicant lists are private to the posting company's own OWNER/RECRUITER —
    // authority is checked in JobApplicationService (Optional empty -> 403), the
    // same "soft failure" pattern as postJobOffer/apply. Note this sub-resource
    // must stay locked down in SecurityConfig despite the broad public GET on
    // /api/job-offers/**.
    @GetMapping("/{id}/applications")
    public ResponseEntity<Page<JobApplicationDTO>> getApplicationsForOffer(@PathVariable Long id,
                                                                            @AuthenticationPrincipal UserDetails principal,
                                                                            @PageableDefault Pageable pageable) {
        return jobApplicationService.getApplicationsForOffer(principal.getUsername(), id, pageable)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Named business action (apply), not a generic sub-resource POST — mirrors the
    // /approve, /reject pattern in AdminController (security/auditability rationale).
    @PostMapping("/{id}/apply")
    public ResponseEntity<JobApplicationDTO> apply(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails principal,
                                                    @RequestBody @Valid ApplyToJobOfferDTO dto) {
        return jobApplicationService.apply(principal.getUsername(), id, dto)
                .map(application -> ResponseEntity.status(HttpStatus.CREATED).body(application))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
