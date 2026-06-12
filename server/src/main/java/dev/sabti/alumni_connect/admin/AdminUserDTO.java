package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import lombok.Data;

import java.time.LocalDateTime;

// Admin-facing view of a User for the moderation browse. Unlike the public profile DTOs, this
// deliberately exposes the moderation fields they hide — userStatus, statusChangeReason,
// emailVerified, createdAt — since the admin needs them to decide who to suspend/reactivate.
// passwordHash is simply never mapped here.
@Data
public class AdminUserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserType userType;
    private UserStatus userStatus;
    private String statusChangeReason;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserDTO from(User user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.id = user.getId();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.email = user.getEmail();
        dto.phoneNumber = user.getPhoneNumber();
        dto.userType = user.getUserType();
        dto.userStatus = user.getUserStatus();
        dto.statusChangeReason = user.getStatusChangeReason();
        dto.emailVerified = user.getEmailVerified();
        dto.createdAt = user.getCreatedAt();
        dto.updatedAt = user.getUpdatedAt();
        return dto;
    }
}
