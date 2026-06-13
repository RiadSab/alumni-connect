package dev.sabti.alumni_connect.company;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<Page<CompanyDTO>> getActiveCompanies(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(companyService.getActiveCompanies(pageable));
    }

    // Public single-company profile — the company page a candidate reaches by clicking the
    // company on a job offer (JobOfferDTO only carries companyId + companyName). Permitted in
    // SecurityConfig by the GET /api/companies/** matcher. Empty -> 404 (no such id, or the
    // company isn't publicly visible — indistinguishable on purpose).
    @GetMapping("/{id}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        return companyService.getVisibleCompanyById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // A company OWNER edits their own company profile. Scoped to the caller's own company (resolved
    // from their CompanyUserProfile in the service), so no path id. OWNER-only authority is checked
    // in the service; Optional empty -> 403, the same soft-failure pattern as the role-change
    // endpoint. (PATCH, so it isn't caught by the public GET /api/companies/** matcher.)
    @PatchMapping("/me")
    public ResponseEntity<CompanyDTO> updateMyCompany(@AuthenticationPrincipal UserDetails principal,
                                                      @RequestBody @Valid UpdateCompanyDTO dto) {
        return companyService.updateMyCompany(principal.getUsername(), dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}
