package dev.sabti.alumni_connect.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Everything is scoped to the caller: there is no way to read anyone else's notifications.
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationDTO> getMine(@AuthenticationPrincipal UserDetails principal,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.getMine(principal.getUsername(), pageable);
    }

    @GetMapping("/unread")
    public List<NotificationDTO> getMyUnread(@AuthenticationPrincipal UserDetails principal) {
        return notificationService.getMyUnread(principal.getUsername());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getMyUnreadCount(@AuthenticationPrincipal UserDetails principal) {
        return Map.of("count", notificationService.getMyUnreadCount(principal.getUsername()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable Long id) {
        notificationService.markRead(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal UserDetails principal) {
        return Map.of("read", notificationService.markAllRead(principal.getUsername()));
    }
}
