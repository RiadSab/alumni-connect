package dev.sabti.alumni_connect.job.saved;

import dev.sabti.alumni_connect.job.offers.JobOfferDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Candidate bookmarks. Whole resource is ROLE CANDIDATE in SecurityConfig. See docs/saved-jobs.md.
@RestController
@RequestMapping("/api/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {
    private final SavedJobService savedJobService;

    @GetMapping
    public Page<JobOfferDTO> getMySavedOffers(@AuthenticationPrincipal UserDetails principal,
                                              @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return savedJobService.getMySavedOffers(principal.getUsername(), pageable);
    }

    // The saved offer ids, so the board can mark each card's bookmark state in one call.
    @GetMapping("/ids")
    public List<Long> getMySavedOfferIds(@AuthenticationPrincipal UserDetails principal) {
        return savedJobService.getMySavedOfferIds(principal.getUsername());
    }

    @PostMapping("/{offerId}")
    public ResponseEntity<JobOfferDTO> save(@PathVariable Long offerId,
                                            @AuthenticationPrincipal UserDetails principal) {
        JobOfferDTO offer = savedJobService.save(principal.getUsername(), offerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(offer);
    }

    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> unsave(@PathVariable Long offerId,
                                       @AuthenticationPrincipal UserDetails principal) {
        savedJobService.unsave(principal.getUsername(), offerId);
        return ResponseEntity.noContent().build();
    }
}
