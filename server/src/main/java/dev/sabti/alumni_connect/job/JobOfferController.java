package dev.sabti.alumni_connect.job;

import dev.sabti.alumni_connect.job.entities.JobOffer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-offers")
@RequiredArgsConstructor
public class JobOfferController {
    private final JobOfferService jobOfferService;

    @PostMapping
    public ResponseEntity<JobOffer> postJobOffer(@AuthenticationPrincipal UserDetails principal,
                                                  @RequestBody @Valid CreateJobOfferDTO dto) {
        return jobOfferService.postJobOffer(principal.getUsername(), dto)
                .map(offer -> ResponseEntity.status(HttpStatus.CREATED).body(offer))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}
