package dev.sabti.alumni_connect.alumni;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkAccountDTO {
    @NotNull(message = "Field 'userId' is required")
    private Long userId;
}
