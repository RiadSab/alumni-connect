package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanySize;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import lombok.Data;

import java.time.LocalDateTime;

// Public-facing view of a Company, returned by GET /api/companies/{id}. Deliberately omits
// statusChangeReason (an internal admin note on rejection/suspension — must never reach the
// public) and videoPresentationId (deferred). logoId is the handle the client uses to fetch the
// logo image from GET /api/companies/{id}/logo. status is kept: only publicly visible statuses
// ever reach here, so it's informative (e.g. a PARTNER badge), not a leak.
@Data
public class CompanyDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Fields field;
    private String description;
    private String website;
    private String address;
    private CompanySize size;
    private String logoId;
    private CompanyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyDTO from(Company company) {
        CompanyDTO dto = new CompanyDTO();
        dto.id = company.getId();
        dto.name = company.getName();
        dto.email = company.getEmail();
        dto.phone = company.getPhone();
        dto.field = company.getField();
        dto.description = company.getDescription();
        dto.website = company.getWebsite();
        dto.address = company.getAddress();
        dto.size = company.getSize();
        dto.logoId = company.getLogoId();
        dto.status = company.getStatus();
        dto.createdAt = company.getCreatedAt();
        dto.updatedAt = company.getUpdatedAt();
        return dto;
    }
}
