package dev.sabti.alumni_connect.alumni;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Public: the caller proves who they are with the one-time token from the claim email.
@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
public class AlumniClaimController {
    private final AlumniClaimService alumniClaimService;

    @GetMapping("/claim/{token}")
    public ResponseEntity<AlumniClaimDetailsDTO> getClaimDetails(@PathVariable String token) {
        return ResponseEntity.ok(alumniClaimService.getDetails(token));
    }

    @PostMapping("/claim")
    public ResponseEntity<Void> claim(@RequestBody @Valid ClaimAccountDTO dto) {
        alumniClaimService.claim(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // A button on the claim page, not a link in the email: mail scanners prefetch links.
    @PostMapping("/claim/{token}/opt-out")
    public ResponseEntity<Void> optOut(@PathVariable String token) {
        alumniClaimService.optOut(token);
        return ResponseEntity.noContent().build();
    }
}
