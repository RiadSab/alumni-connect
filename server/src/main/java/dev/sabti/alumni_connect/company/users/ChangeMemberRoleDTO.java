package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.company.entities.CompanyRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// The new platform role for a company member. OWNER is a valid enum value but is rejected by
// the service for this endpoint (granting OWNER is ownership transfer, a separate operation),
// so it's guarded in business logic rather than excluded at validation.
@Data
public class ChangeMemberRoleDTO {
    @NotNull(message = "Field 'role' is required")
    private CompanyRole role;
}
