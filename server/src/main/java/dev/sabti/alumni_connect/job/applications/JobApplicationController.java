package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.candidate.CandidateProfileDTO;
import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;


    @GetMapping("/me")
    public Page<JobApplicationDTO> getMyApplications(@AuthenticationPrincipal UserDetails principal,
                                                     @RequestParam(required = false) List<ApplicationStatus> status,
                                                     @PageableDefault Pageable pageable) {
        return jobApplicationService.getMyApplications(principal.getUsername(), status, pageable);
    }

    // Candidate dashboard counts — the aggregated companion to /me.
    @GetMapping("/me/stats")
    public MyApplicationStatsDTO getMyApplicationStats(@AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getMyApplicationStats(principal.getUsername());
    }

    @GetMapping("/{id}")
    public JobApplicationDTO getApplicationById(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getApplicationById(principal.getUsername(), id);
    }

    @GetMapping("/{id}/applicant")
    public CandidateProfileDTO getApplicantProfile(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.getApplicantProfile(principal.getUsername(), id);
    }

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

    @PostMapping("/{id}/withdraw")
    public JobApplicationDTO withdraw(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails principal) {
        return jobApplicationService.withdraw(principal.getUsername(), id);
    }


    @PatchMapping("/{id}")
    public JobApplicationDTO review(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails principal,
                                   @RequestBody @Valid ReviewApplicationDTO dto) {
        return jobApplicationService.review(principal.getUsername(), id, dto);
    }
}
