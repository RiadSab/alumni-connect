package dev.sabti.alumni_connect.alumni;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkAccountDTO {
    @NotBlank(message = "Field 'email' is required")
    @Email
    private String email;
}
