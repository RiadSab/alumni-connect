package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.candidate.CandidateProfileDTO;
import dev.sabti.alumni_connect.storage.FileDownload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    // The candidate's own application history, any status — mirrors GET /api/job-offers/me on the
    // company side. The service throws 403 if the caller has no CandidateProfile. Must be registered
    // (Spring matches literal path segments before path variables) before /{id} below, so "/me" isn't
    // parsed as an id.
    @GetMapping("/me")
    public Page<JobApplicationDTO> getMyApplications(@AuthenticationPrincipal UserDetails principal,
                                                     @PageableDefault Pageable pageable) {
        return jobApplicationService.getMyApplications(principal.getUsername(), pageable);
    }

    // Visible only to the applicant or the posting company's own OWNER/RECRUITER — checked in the
    // service. "Not found" and "not yours" both throw 404, so this endpoint never confirms whether an
    // application id you can't access exists.
    @GetMapping("/{id}")
    public JobApplicationDTO getApplicationById(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getApplicationById(principal.getUsername(), id);
    }

    // The applicant's full candidate profile (skills, experience, contact info, links) — lets the
    // posting company's OWNER/RECRUITER review a candidate beyond the name shown on the applications
    // list. Company-only (the applicant reads their own profile via GET /api/candidates/me), checked
    // in the service; a miss (no such application, or caller isn't the posting company) throws 404, so
    // an id you can't reach never leaks.
    @GetMapping("/{id}/applicant")
    public CandidateProfileDTO getApplicantProfile(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getApplicantProfile(principal.getUsername(), id);
    }

    // Stream the resume PDF attached to an application — the read side of apply-with-resume. Same
    // access gate as GET /{id} (the applicant, or the posting company's OWNER/RECRUITER), checked in
    // the service; every miss (no such application, not yours, or no resume on it) throws 404, so an
    // id you can't reach never leaks. Content-Disposition inline so a reviewer can preview the PDF
    // in-browser; the original filename is echoed.
    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadApplicationResume(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails principal) {
        FileDownload file = jobApplicationService.getApplicationResume(principal.getUsername(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getMetadata().getOriginalFilename() + "\"")
                .body(file.getResource());
    }

    // A candidate withdraws their own application. A named action (POST /withdraw), like apply and
    // the admin approve/reject actions — not the PATCH below, which is the company's review. The
    // applicant-only check is in the service; "not yours"/"not found" both throw 404, so other
    // applications aren't probeable. Returns the application with status WITHDRAWN.
    @PostMapping("/{id}/withdraw")
    public JobApplicationDTO withdraw(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.withdraw(principal.getUsername(), id);
    }

    // PATCH (not a named action like /approve) because reviewing genuinely is a partial update of
    // several independent fields at once (status, note, priority, rating) — it doesn't decompose into
    // single-purpose actions the way admin approve/reject does. Authority (same-company
    // OWNER/RECRUITER) is checked in the service, which throws 404 for a missing application or one
    // not belonging to the caller's company.
    @PatchMapping("/{id}")
    public JobApplicationDTO review(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails principal,
                                   @RequestBody @Valid ReviewApplicationDTO dto) {
        return jobApplicationService.review(principal.getUsername(), id, dto);
    }
}
