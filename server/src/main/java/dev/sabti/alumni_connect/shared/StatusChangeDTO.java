package dev.sabti.alumni_connect.shared;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Body for admin approve/reject actions — the target id comes from the URL path,
// this only carries the reason recorded against the entity's statusChangeReason.
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class StatusChangeDTO {
    @NotBlank
    private String reason;
}
