package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanySize;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import lombok.Data;

import java.time.LocalDateTime;

// Admin-facing view of a Company for the moderation browse — exposes the moderation fields the public CompanyDTO hides (status, statusChangeReason).
@Data
public class AdminCompanyDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Fields field;
    private String description;
    private String website;
    private String address;
    private CompanySize size;
    private CompanyStatus status;
    private String statusChangeReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // The company's owner, resolved for the pending queue; null on the browse-all list.
    private OwnerSummary owner;

    public record OwnerSummary(String firstName, String lastName, String email) {}

    public static AdminCompanyDTO from(Company company) {
        return from(company, null);
    }

    public static AdminCompanyDTO from(Company company, CompanyUserProfile owner) {
        AdminCompanyDTO dto = new AdminCompanyDTO();
        dto.id = company.getId();
        dto.name = company.getName();
        dto.email = company.getEmail();
        dto.phone = company.getPhone();
        dto.field = company.getField();
        dto.description = company.getDescription();
        dto.website = company.getWebsite();
        dto.address = company.getAddress();
        dto.size = company.getSize();
        dto.status = company.getStatus();
        dto.statusChangeReason = company.getStatusChangeReason();
        dto.createdAt = company.getCreatedAt();
        dto.updatedAt = company.getUpdatedAt();
        if (owner != null) {
            User user = owner.getUser();
            dto.owner = new OwnerSummary(user.getFirstName(), user.getLastName(), user.getEmail());
        }
        return dto;
    }
}
