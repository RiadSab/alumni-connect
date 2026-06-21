package dev.sabti.alumni_connect.auth.password;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Public (no JWT — the user is locked out). Both fall under the /api/auth/** permitAll.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    // Always 200 (the service never reveals whether the email is registered).
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        passwordResetService.requestReset(dto);
        return ResponseEntity.ok().build();
    }

    // 400 "Invalid or expired code" on any failure; 200 on success.
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        passwordResetService.reset(dto);
        return ResponseEntity.ok().build();
    }
}
