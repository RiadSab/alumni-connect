package dev.sabti.alumni_connect.employment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 403 if the caller has no candidate profile — thrown by the service, same as /api/candidates/me.
@RestController
@RequestMapping("/api/employment")
@RequiredArgsConstructor
public class EmploymentController {
    private final EmploymentService employmentService;

    @GetMapping("/me")
    public List<EmploymentEntryDTO> getMyEntries(@AuthenticationPrincipal UserDetails principal) {
        return employmentService.getMyEntries(principal.getUsername());
    }

    @PostMapping("/me")
    public ResponseEntity<EmploymentEntryDTO> create(@AuthenticationPrincipal UserDetails principal,
                                                     @RequestBody @Valid SaveEmploymentEntryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employmentService.create(principal.getUsername(), dto));
    }

    @PatchMapping("/me/{id}")
    public EmploymentEntryDTO update(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id,
                                     @RequestBody @Valid SaveEmploymentEntryDTO dto) {
        return employmentService.update(principal.getUsername(), id, dto);
    }

    @DeleteMapping("/me/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        employmentService.delete(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
