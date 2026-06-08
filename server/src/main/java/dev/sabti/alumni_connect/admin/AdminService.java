package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public Page<User> getPendingUsers(Pageable pageable) {
        return userRepository.findByUserStatus(UserStatus.PENDING, pageable);
    }

    public Page<Company> getPendingCompanies(Pageable pageable) {
        return companyRepository.findByStatus(CompanyStatus.PENDING, pageable);
    }

    // Single status-change path for every User regardless of role (CANDIDATE, COMPANY_USER,
    // ADMINISTRATOR) — composition means they're all the same entity, so one method covers them all.
    @Transactional
    public boolean changeUserStatus(Long id, String reason, UserStatus newStatus) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null || user.getUserStatus() == newStatus) {
            return false;
        }
        user.setUserStatus(newStatus);
        user.setStatusChangeReason(reason);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean changeCompanyStatus(Long id, String reason, CompanyStatus newStatus) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null || company.getStatus() == newStatus) {
            return false;
        }
        company.setStatus(newStatus);
        company.setStatusChangeReason(reason);
        companyRepository.save(company);
        return true;
    }
}
