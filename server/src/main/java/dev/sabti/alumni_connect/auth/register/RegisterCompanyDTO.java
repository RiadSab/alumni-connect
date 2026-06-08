package dev.sabti.alumni_connect.auth.register;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.company.entities.CompanySize;
import dev.sabti.alumni_connect.company.entities.CompanyUserPosition;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Logo/video uploads deliberately left out — same call as RegisterCandidateDTO, deferred to
// file-storage. Creates the company's first user (the OWNER) and the Company together in one
// shot; companyRole/companyId are never accepted from the client — the role is hardcoded to
// OWNER server-side and the company is being created here, not joined.
@Data
public class RegisterCompanyDTO {

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

    @NotBlank(message = "Field 'companyName' is required")
    private String companyName;

    @NotBlank(message = "Field 'companyEmail' is required")
    @Email(message = "Field 'companyEmail' must be a valid email")
    private String companyEmail;

    private String companyPhone;

    @NotNull(message = "Field 'companyField' is required")
    private Fields companyField;

    private String companyDescription;
    private String companyWebsite;
    private String companyAddress;
    private CompanySize companySize;
}
