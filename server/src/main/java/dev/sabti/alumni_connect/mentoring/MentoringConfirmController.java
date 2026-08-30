package dev.sabti.alumni_connect.mentoring;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Public: the token in the emailed link is the authentication. Confirming is a POST from a button
// on the page — mail scanners follow links, and one would otherwise confirm on the student's behalf.
@RestController
@RequestMapping("/api/mentoring/confirm")
@RequiredArgsConstructor
public class MentoringConfirmController {
    private final MentoringSessionService sessionService;

    @GetMapping("/{token}")
    public MentoringBookingDTO getDetails(@PathVariable String token) {
        return sessionService.getConfirmDetails(token);
    }

    @PostMapping("/{token}")
    public ResponseEntity<Void> confirm(@PathVariable String token) {
        sessionService.confirm(token);
        return ResponseEntity.noContent().build();
    }
}
