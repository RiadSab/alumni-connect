package dev.sabti.alumni_connect.auth.password;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ChangePasswordController {
    private final ChangePasswordService changePasswordService;

    // Authenticated (the principal is the user changing their own password) — gated in
    // SecurityConfig with an authenticated matcher declared before the /api/auth/** permitAll,
    // since everything else under /api/auth (login, register) is public. The service throws 400
    // "Current password is incorrect" if the old password doesn't match — the one failure the caller
    // can correct.
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails principal,
                                               @RequestBody @Valid ChangePasswordDTO dto) {
        changePasswordService.changePassword(principal.getUsername(), dto);
        return ResponseEntity.ok().build();
    }
}
