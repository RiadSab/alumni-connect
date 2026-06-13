package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.candidate.CandidateProfileDTO;
import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    // The candidate's own application history, any status — mirrors
    // GET /api/job-offers/me on the company side. Empty -> 403 if the caller has no
    // CandidateProfile. Must be registered (Spring matches literal path segments
    // before path variables) before /{id} below, so "/me" isn't parsed as an id.
    @GetMapping("/me")
    public ResponseEntity<Page<JobApplicationDTO>> getMyApplications(@AuthenticationPrincipal UserDetails principal,
                                                                       @PageableDefault Pageable pageable) {
        return jobApplicationService.getMyApplications(principal.getUsername(), pageable)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Visible only to the applicant or the posting company's own OWNER/RECRUITER —
    // checked in the service. "Not found" and "not yours" both -> 404, so this
    // endpoint never confirms whether an application id you can't access exists.
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationDTO> getApplicationById(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getApplicationById(principal.getUsername(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // The applicant's full candidate profile (skills, experience, contact info, links) — lets the
    // posting company's OWNER/RECRUITER review a candidate beyond the name shown on the applications
    // list. Company-only (the applicant reads their own profile via GET /api/candidates/me), checked
    // in the service; a miss (no such application, or caller isn't the posting company) -> 404, so an
    // id you can't reach never leaks.
    @GetMapping("/{id}/applicant")
    public ResponseEntity<CandidateProfileDTO> getApplicantProfile(@PathVariable Long id,
                                                                   @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getApplicantProfile(principal.getUsername(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Stream the resume PDF attached to an application — the read side of apply-with-resume.
    // Same access gate as GET /{id} (the applicant, or the posting company's OWNER/RECRUITER),
    // checked in the service; every miss (no such application, not yours, or no resume on it)
    // -> 404, so an id you can't reach never leaks. Content-Disposition inline so a reviewer can
    // preview the PDF in-browser; the original filename is echoed.
    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadApplicationResume(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails principal) {
        Optional<FileDownload> result = jobApplicationService.getApplicationResume(principal.getUsername(), id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileDownload download = result.get();
        StoredFile metadata = download.getMetadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(download.getResource());
    }

    // PATCH (not a named action like /approve) because reviewing genuinely is a
    // partial update of several independent fields at once (status, note, priority,
    // rating) — it doesn't decompose into single-purpose actions the way admin
    // approve/reject does. Authority (same-company OWNER/RECRUITER) is checked in
    // the service; Optional empty -> 403.
    @PatchMapping("/{id}")
    public ResponseEntity<JobApplicationDTO> review(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails principal,
                                                     @RequestBody @Valid ReviewApplicationDTO dto) {
        return jobApplicationService.review(principal.getUsername(), id, dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}
