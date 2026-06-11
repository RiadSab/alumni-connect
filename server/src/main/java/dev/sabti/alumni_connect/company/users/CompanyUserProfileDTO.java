package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserPosition;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import lombok.Data;

import java.time.LocalDateTime;

// Flattens User identity fields together with CompanyUserProfile + company data —
// mirrors CandidateProfileDTO's shape (same pattern for /me and a future admin /{id}).
@Data
public class CompanyUserProfileDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserStatus userStatus;
    private Long companyId;
    private String companyName;
    private CompanyRole companyRole;
    private CompanyUserPosition position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyUserProfileDTO from(User user, CompanyUserProfile profile) {
        CompanyUserProfileDTO dto = new CompanyUserProfileDTO();
        dto.id = user.getId();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.email = user.getEmail();
        dto.phoneNumber = user.getPhoneNumber();
        dto.userStatus = user.getUserStatus();
        dto.companyId = profile.getCompany().getId();
        dto.companyName = profile.getCompany().getName();
        dto.companyRole = profile.getCompanyRole();
        dto.position = profile.getPosition();
        dto.createdAt = profile.getCreatedAt();
        dto.updatedAt = profile.getUpdatedAt();
        return dto;
    }
}
