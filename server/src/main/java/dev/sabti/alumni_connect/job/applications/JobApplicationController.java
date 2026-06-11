package dev.sabti.alumni_connect.job.applications;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

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
