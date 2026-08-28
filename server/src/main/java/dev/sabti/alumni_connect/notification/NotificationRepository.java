package dev.sabti.alumni_connect.notification;

import dev.sabti.alumni_connect.auth.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);

    long countByUserAndReadAtIsNull(User user);

    Optional<Notification> findByIdAndUser(Long id, User user);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.user = :user and n.readAt is null")
    int markAllRead(@Param("user") User user, @Param("readAt") LocalDateTime readAt);
}
