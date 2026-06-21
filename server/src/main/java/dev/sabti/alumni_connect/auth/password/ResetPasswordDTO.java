package dev.sabti.alumni_connect.auth.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "Field 'email' is required")
    @Email
    private String email;

    @NotBlank(message = "Field 'code' is required")
    private String code;

    @NotBlank(message = "Field 'newPassword' is required")
    private String newPassword;
}
