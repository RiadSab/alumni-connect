package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.company.entities.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<Page<Company>> getActiveCompanies(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(companyService.getActiveCompanies(pageable));
    }
}
