package dev.sabti.alumni_connect.employment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Public: the token in the link is the authentication. Permitted in SecurityConfig.
@RestController
@RequestMapping("/api/employment/confirm")
@RequiredArgsConstructor
public class EmploymentConfirmController {
    private final EmploymentNudgeService employmentNudgeService;

    @GetMapping("/{token}")
    public EmploymentConfirmDetailsDTO getDetails(@PathVariable String token) {
        return employmentNudgeService.getDetails(token);
    }

    // A button on the page, not a link in the email: mail scanners prefetch links and would
    // confirm on the person's behalf.
    @PostMapping("/{token}")
    public ResponseEntity<Void> confirm(@PathVariable String token) {
        employmentNudgeService.confirm(token);
        return ResponseEntity.noContent().build();
    }
}
