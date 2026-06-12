package dev.sabti.alumni_connect.company;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
