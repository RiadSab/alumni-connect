package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;

    // Candidates only, company owners are approved with their company, members by their owner.
    public Page<AdminUserDTO> getPendingUsers(Pageable pageable) {
        return userRepository.findByUserStatusAndUserType(UserStatus.PENDING, UserType.CANDIDATE, pageable)
                .map(AdminUserDTO::from);
    }

    // Admin dashboard counts via aggregate queries
    @Transactional(readOnly = true)
    public AdminStatsDTO getStats() {
        long pendingUsers = userRepository.countByUserStatusAndUserType(UserStatus.PENDING, UserType.CANDIDATE);
        long pendingCompanies = companyRepository.countByStatus(CompanyStatus.PENDING);
        return new AdminStatsDTO(pendingUsers, pendingCompanies);
    }

    // Admin moderation browse, no filter = all
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getUsers(UserStatus status, UserType type, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and(UserSpecs.hasStatus(status));
        }
        if (type != null) {
            spec = spec.and(UserSpecs.hasType(type));
        }
        return userRepository.findAll(spec, pageable).map(AdminUserDTO::from);
    }

    // Each pending company carries its owner so the admin reviews and approves them as one unit.
    @Transactional(readOnly = true)
    public Page<AdminCompanyDTO> getPendingCompanies(Pageable pageable) {
        return companyRepository.findByStatus(CompanyStatus.PENDING, pageable)
                .map(company -> AdminCompanyDTO.from(company, findOwner(company)));
    }

    // Single-owner model — at most one OWNER profile per company.
    private CompanyUserProfile findOwner(Company company) {
        return companyUserProfileRepository.findByCompanyAndCompanyRole(company, CompanyRole.OWNER)
                .stream().findFirst().orElse(null);
    }

    // Admin moderation browse over companies, optionally narrowed by status (no filter = all).
    @Transactional(readOnly = true)
    public Page<AdminCompanyDTO> getCompanies(CompanyStatus status, Pageable pageable) {
        Page<Company> companies = (status == null)
                ? companyRepository.findAll(pageable)
                : companyRepository.findByStatus(status, pageable);
        return companies.map(AdminCompanyDTO::from);
    }

    // One status-change path for every User; missing user -> 404, already in target status -> 409.
    @Transactional
    public void changeUserStatus(Long id, String reason, UserStatus newStatus) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getUserStatus() == newStatus) {
            throw new ConflictException("User is already " + newStatus);
        }
        // An owner can't be activated on their own while their company is still pending
        if (newStatus == UserStatus.ACTIVE) {
            companyUserProfileRepository.findByUser(user).ifPresent(profile -> {
                if (profile.getCompanyRole() == CompanyRole.OWNER
                        && profile.getCompany().getStatus() == CompanyStatus.PENDING) {
                    throw new ConflictException("Approve the company before activating its owner");
                }
            });
        }
        user.setUserStatus(newStatus);
        user.setStatusChangeReason(reason);
        userRepository.save(user);
    }

    @Transactional
    public void changeCompanyStatus(Long id, String reason, CompanyStatus newStatus) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (company.getStatus() == newStatus) {
            throw new ConflictException("Company is already " + newStatus);
        }
        company.setStatus(newStatus);
        company.setStatusChangeReason(reason);
        companyRepository.save(company);

        // Approving the company activates its still-pending owner
        if (newStatus == CompanyStatus.ACTIVE) {
            CompanyUserProfile owner = findOwner(company);
            if (owner != null && owner.getUser().getUserStatus() == UserStatus.PENDING) {
                owner.getUser().setUserStatus(UserStatus.ACTIVE);
                userRepository.save(owner.getUser());
            }
        }
    }
}
