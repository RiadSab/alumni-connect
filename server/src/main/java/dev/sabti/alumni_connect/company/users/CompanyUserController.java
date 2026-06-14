package dev.sabti.alumni_connect.company.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    // The caller's own company roster (the list an OWNER picks a member from before changing
    // their role). Scoped to the caller's company by identity — no company id in the path, so
    // no one can list another company's members. The service throws 403 if the caller isn't a
    // company user.
    @GetMapping
    public Page<CompanyUserProfileDTO> getMyCompanyMembers(@AuthenticationPrincipal UserDetails principal,
                                                           @PageableDefault Pageable pageable) {
        return companyUserService.getMyCompanyMembers(principal.getUsername(), pageable);
    }

    // The service throws 403 if the caller isn't a company user (no CompanyUserProfile).
    @GetMapping("/me")
    public CompanyUserProfileDTO getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return companyUserService.getMyProfile(principal.getUsername());
    }

    @PatchMapping("/me")
    public CompanyUserProfileDTO updateMyProfile(@AuthenticationPrincipal UserDetails principal,
                                                 @RequestBody @Valid UpdateCompanyUserProfileDTO dto) {
        return companyUserService.updateMyProfile(principal.getUsername(), dto);
    }

    // Admin-only: lookup by User id, e.g. reviewing a pending company-user from
    // /api/admin/pending-users before approve/reject. Restricted to ADMINISTRATOR
    // in SecurityConfig (declared after /me so "/me" isn't shadowed by this matcher). The service
    // throws 404 if the id doesn't exist or isn't a company user.
    @GetMapping("/{id}")
    public CompanyUserProfileDTO getProfileById(@PathVariable Long id) {
        return companyUserService.getProfileById(id);
    }

    // A company OWNER changes another member's platform role (v1: MEMBER <-> RECRUITER).
    // {id} is the target member's User id, consistent with GET /{id}. The "OWNER of the same
    // company" authority is too fine-grained for hasRole, so SecurityConfig only requires
    // authentication and the service decides — throwing 403 (not owner / own role / granting OWNER)
    // or 404 (target not in the actor's company), each with its own message.
    @PatchMapping("/{id}/role")
    public CompanyUserProfileDTO changeMemberRole(@AuthenticationPrincipal UserDetails principal,
                                                  @PathVariable Long id,
                                                  @RequestBody @Valid ChangeMemberRoleDTO dto) {
        return companyUserService.changeMemberRole(principal.getUsername(), id, dto.getRole());
    }
}
