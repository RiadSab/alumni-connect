package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;

    // Statuses a company may be seen by the public under. ACTIVE and PARTNER are both "live,
    // admin-approved" companies; PENDING/REJECTED/SUSPENDED stay hidden so we neither expose a
    // rejected company's existence nor a suspended company's profile.
    private static final Set<CompanyStatus> PUBLICLY_VISIBLE =
            EnumSet.of(CompanyStatus.ACTIVE, CompanyStatus.PARTNER);

    // Only ACTIVE companies are discoverable here — this list feeds the "join an existing
    // company" registration flow, and PENDING/REJECTED/SUSPENDED companies must stay invisible
    // to people who aren't members yet.
    public Page<CompanyDTO> getActiveCompanies(Pageable pageable) {
        return companyRepository.findByStatus(CompanyStatus.ACTIVE, pageable).map(CompanyDTO::from);
    }

    // Public single-company profile (the company page reached from a job offer / directory).
    // Empty -> 404 for both "no such id" and "exists but not publicly visible" — same
    // 404-for-both reasoning as the private read endpoints: a caller must not be able to tell a
    // suspended/rejected company apart from one that never existed.
    @Transactional(readOnly = true)
    public Optional<CompanyDTO> getVisibleCompanyById(Long id) {
        return companyRepository.findById(id)
                .filter(company -> PUBLICLY_VISIBLE.contains(company.getStatus()))
                .map(CompanyDTO::from);
    }
}
