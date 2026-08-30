package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.auth.entities.Fields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Who may do what is decided in the service: publishing needs a claimed roster row, booking needs
// a student profile. Both are 403s, not route rules, because they depend on the person's data.
@RestController
@RequestMapping("/api/mentoring")
@RequiredArgsConstructor
public class MentoringController {
    private final MentoringService mentoringService;

    @GetMapping("/slots")
    public Page<MentoringSlotDTO> browse(@RequestParam(required = false) MentoringTrack track,
                                         @RequestParam(required = false) Fields field,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return mentoringService.browse(track, field, pageable);
    }

    @PostMapping("/slots")
    public ResponseEntity<MentoringSlotDTO> publish(@AuthenticationPrincipal UserDetails principal,
                                                    @RequestBody @Valid PublishSlotDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mentoringService.publish(principal.getUsername(), dto));
    }

    @GetMapping("/slots/me")
    public List<MentoringSlotDTO> getMySlots(@AuthenticationPrincipal UserDetails principal) {
        return mentoringService.getMySlots(principal.getUsername());
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> cancelSlot(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        mentoringService.cancelSlot(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/slots/{id}/book")
    public ResponseEntity<MentoringBookingDTO> book(@AuthenticationPrincipal UserDetails principal,
                                                    @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mentoringService.book(principal.getUsername(), id));
    }

    @GetMapping("/bookings/me")
    public List<MentoringBookingDTO> getMyBookings(@AuthenticationPrincipal UserDetails principal) {
        return mentoringService.getMyBookings(principal.getUsername());
    }

    // Drives the "2 sessions left this semester" line on the booking page.
    @GetMapping("/bookings/me/remaining")
    public Map<String, Long> remaining(@AuthenticationPrincipal UserDetails principal) {
        return Map.of("remaining", mentoringService.remainingBookings(principal.getUsername()));
    }

    @GetMapping("/bookings/mentor")
    public List<MentoringBookingDTO> getMentorBookings(@AuthenticationPrincipal UserDetails principal) {
        return mentoringService.getMentorBookings(principal.getUsername());
    }

    @PostMapping("/bookings/{id}/attendance")
    public ResponseEntity<Void> markAttendance(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long id,
                                               @RequestParam boolean attended) {
        mentoringService.markAttendance(principal.getUsername(), id, attended);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        mentoringService.cancelBooking(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
