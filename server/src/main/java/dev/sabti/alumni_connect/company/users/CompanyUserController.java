package dev.sabti.alumni_connect.company.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
}
