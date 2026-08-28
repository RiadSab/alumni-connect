package dev.sabti.alumni_connect.notification;

import java.time.LocalDateTime;

public record NotificationDTO(Long id, NotificationType type, String subject, String context,
                              String link, LocalDateTime createdAt, LocalDateTime readAt) {

    public static NotificationDTO from(Notification notification) {
        return new NotificationDTO(notification.getId(), notification.getType(),
                notification.getSubject(), notification.getContext(), notification.getLink(),
                notification.getCreatedAt(), notification.getReadAt());
    }
}
