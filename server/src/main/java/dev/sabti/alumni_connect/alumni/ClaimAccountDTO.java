package dev.sabti.alumni_connect.alumni;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimAccountDTO {
    @NotBlank(message = "Field 'token' is required")
    private String token;

    @NotBlank(message = "Field 'password' is required")
    private String password;

    private String phoneNumber;
}
