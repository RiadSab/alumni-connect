package dev.sabti.alumni_connect.company.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-users")
@RequiredArgsConstructor
public class CompanyUserController {
    private final CompanyUserService companyUserService;

    // Empty -> 403 if the caller isn't a company user (no CompanyUserProfile).
    @GetMapping("/me")
    public ResponseEntity<CompanyUserProfileDTO> getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return companyUserService.getMyProfile(principal.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PatchMapping("/me")
    public ResponseEntity<CompanyUserProfileDTO> updateMyProfile(@AuthenticationPrincipal UserDetails principal,
                                                                   @RequestBody @Valid UpdateCompanyUserProfileDTO dto) {
        return companyUserService.updateMyProfile(principal.getUsername(), dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Admin-only: lookup by User id, e.g. reviewing a pending company-user from
    // /api/admin/pending-users before approve/reject. Restricted to ADMINISTRATOR
    // in SecurityConfig (declared after /me so "/me" isn't shadowed by this matcher).
    @GetMapping("/{id}")
    public ResponseEntity<CompanyUserProfileDTO> getProfileById(@PathVariable Long id) {
        return companyUserService.getProfileById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // A company OWNER changes another member's platform role (v1: MEMBER <-> RECRUITER).
    // {id} is the target member's User id, consistent with GET /{id}. The "OWNER of the same
    // company" authority is too fine-grained for hasRole, so SecurityConfig only requires
    // authentication and the service decides — returning an Outcome the controller maps to
    // 200/403/404 (403 vs 404 is a real distinction here, hence a result type not an Optional).
    @PatchMapping("/{id}/role")
    public ResponseEntity<CompanyUserProfileDTO> changeMemberRole(@AuthenticationPrincipal UserDetails principal,
                                                                  @PathVariable Long id,
                                                                  @RequestBody @Valid ChangeMemberRoleDTO dto) {
        ChangeMemberRoleResult result = companyUserService.changeMemberRole(principal.getUsername(), id, dto.getRole());
        return switch (result.outcome()) {
            case SUCCESS -> ResponseEntity.ok(result.profile());
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }
}
