package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;

    // Only ACTIVE companies are discoverable here — this list feeds the "join an existing
    // company" registration flow, and PENDING/REJECTED/SUSPENDED companies must stay invisible
    // to people who aren't members yet.
    public Page<Company> getActiveCompanies(Pageable pageable) {
        return companyRepository.findByStatus(CompanyStatus.ACTIVE, pageable);
    }
}
