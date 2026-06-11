package dev.sabti.alumni_connect.auth.password;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Both fields required. Password strength isn't constrained here on purpose — registration
// (RegisterCandidateDTO/RegisterCompanyDTO) only enforces @NotBlank on password too, and a
// change-password endpoint applying a stricter rule than the one used to create the account
// would be inconsistent. Tighten both together if/when a password policy is introduced.
@Data
public class ChangePasswordDTO {
    @NotBlank(message = "Field 'oldPassword' is required")
    private String oldPassword;

    @NotBlank(message = "Field 'newPassword' is required")
    private String newPassword;
}
