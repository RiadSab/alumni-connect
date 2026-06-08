package dev.sabti.alumni_connect.auth.register;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/register")
@RequiredArgsConstructor
public class RegisterController {
    private final RegisterService registerService;

    @PostMapping("/candidate")
    public ResponseEntity<Void> registerCandidate(@RequestBody @Valid RegisterCandidateDTO dto) {
        registerService.registerCandidate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/company")
    public ResponseEntity<Void> registerCompany(@RequestBody @Valid RegisterCompanyDTO dto) {
        registerService.registerCompanyOwner(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
