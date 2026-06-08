package dev.sabti.alumni_connect.auth.register;

import dev.sabti.alumni_connect.company.entities.CompanyUserPosition;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Joins an existing, already-vetted Company — the candidate picks one from
// GET /api/companies (ACTIVE only). companyRole is never accepted here: every
// self-registered joiner becomes a MEMBER, the safe default. Elevating someone to
// RECRUITER is left to that company's own OWNER once member-management exists —
// neither the registrant nor the platform admin is positioned to make that call.
@Data
public class RegisterCompanyMemberDTO {

    @NotBlank(message = "Field 'firstName' is required")
    private String firstName;

    @NotBlank(message = "Field 'lastName' is required")
    private String lastName;

    @NotBlank(message = "Field 'email' is required")
    @Email(message = "Field 'email' must be a valid email")
    private String email;

    @NotBlank(message = "Field 'password' is required")
    private String password;

    private String phoneNumber;

    @NotNull(message = "Field 'position' is required")
    private CompanyUserPosition position;

    @NotNull(message = "Field 'companyId' is required")
    private Long companyId;
}
