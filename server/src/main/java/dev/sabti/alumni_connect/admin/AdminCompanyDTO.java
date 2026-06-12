package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanySize;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import lombok.Data;

import java.time.LocalDateTime;

// Admin-facing view of a Company for the moderation browse. Unlike the public CompanyDTO (which
// is visibility-gated and omits status/statusChangeReason), this exposes those moderation fields
// — the admin must see PENDING/REJECTED/SUSPENDED companies and the reason behind a status.
// logoId/videoPresentationId stay omitted until the file-storage phase, as elsewhere.
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

    public static AdminCompanyDTO from(Company company) {
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
        return dto;
    }
}
