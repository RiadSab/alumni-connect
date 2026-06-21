package dev.sabti.alumni_connect.auth.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordDTO {
    @NotBlank(message = "Field 'email' is required")
    @Email
    private String email;
}
