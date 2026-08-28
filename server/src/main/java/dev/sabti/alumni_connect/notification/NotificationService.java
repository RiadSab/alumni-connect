package dev.sabti.alumni_connect.notification;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Called from wherever the event happens; never fails the action that caused it.
    @Transactional
    public void notify(User user, NotificationType type, String subject, String context, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setSubject(subject);
        notification.setContext(context);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getMine(String email, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(userOf(email), pageable)
                .map(NotificationDTO::from);
    }

    // The dashboard shows these; clicking one is what makes it disappear from there.
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyUnread(String email) {
        return notificationRepository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(userOf(email))
                .stream()
                .map(NotificationDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getMyUnreadCount(String email) {
        return notificationRepository.countByUserAndReadAtIsNull(userOf(email));
    }

    @Transactional
    public void markRead(String email, Long id) {
        Notification notification = notificationRepository.findByIdAndUser(id, userOf(email))
                .orElseThrow(() -> new NotFoundException("Notification not found")); // not yours -> 404
        // Already read: leave the original timestamp alone.
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public int markAllRead(String email) {
        return notificationRepository.markAllRead(userOf(email), LocalDateTime.now());
    }

    private User userOf(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Not a user"));
    }
}
