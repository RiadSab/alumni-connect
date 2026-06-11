package dev.sabti.alumni_connect.candidate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {
    private final CandidateService candidateService;

    // Empty -> 403 if the caller isn't a candidate (no CandidateProfile). Must be
    // registered (Spring matches literal path segments before path variables) before
    // /{id} below, so "/me" isn't parsed as a user id.
    @GetMapping("/me")
    public ResponseEntity<CandidateProfileDTO> getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return candidateService.getMyProfile(principal.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PatchMapping("/me")
    public ResponseEntity<CandidateProfileDTO> updateMyProfile(@AuthenticationPrincipal UserDetails principal,
                                                                 @RequestBody @Valid UpdateCandidateProfileDTO dto) {
        return candidateService.updateMyProfile(principal.getUsername(), dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Admin-only: lookup by User id, e.g. reviewing a pending candidate from
    // /api/admin/pending-users before approve/reject. Restricted to ADMINISTRATOR
    // in SecurityConfig (declared after /me so "/me" isn't shadowed by this matcher).
    @GetMapping("/{id}")
    public ResponseEntity<CandidateProfileDTO> getProfileById(@PathVariable Long id) {
        return candidateService.getProfileById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
