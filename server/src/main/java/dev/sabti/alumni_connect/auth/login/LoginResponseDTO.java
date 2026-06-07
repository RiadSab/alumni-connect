package dev.sabti.alumni_connect.auth.login;

import dev.sabti.alumni_connect.auth.entities.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;

// Single response shape for every role — the old project had a separate
// LoginResponse{Candidate,CompanyUser,Administrator}DTO per role bundling dashboard data
// (suggested jobs, applications, company info, platform stats). Those depend on features
// that don't exist yet, so this stays role-agnostic; per-role dashboard data can be fetched
// by the frontend afterwards via the relevant feature's own endpoints.
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserType userType;
}
