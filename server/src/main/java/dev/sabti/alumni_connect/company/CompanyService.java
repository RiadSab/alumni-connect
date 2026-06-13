package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFile;
import dev.sabti.alumni_connect.storage.StoredFileDTO;
import dev.sabti.alumni_connect.storage.StoredFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;
    private final StoredFileService storedFileService;

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

    // A company OWNER edits their own company's public profile — until now a company was write-once
    // (create at registration, then frozen), unlike candidates who already have PATCH /me. Scoped to
    // the caller's own company (resolved from their CompanyUserProfile), so there's no path id to
    // reconcile. OWNER-only: a RECRUITER manages offers/applications, not the company's identity.
    // Empty -> 403 if the caller isn't an OWNER (same soft-failure pattern as changeMemberRole).
    // Fields are all nullable: null means "leave unchanged".
    @Transactional
    public Optional<CompanyDTO> updateMyCompany(String ownerEmail, UpdateCompanyDTO dto) {
        User user = userRepository.findByEmail(ownerEmail).orElse(null);
        if (user == null) return Optional.empty();

        CompanyUserProfile actor = companyUserProfileRepository.findByUser(user).orElse(null);
        if (actor == null || actor.getCompanyRole() != CompanyRole.OWNER) return Optional.empty();

        Company company = actor.getCompany();
        if (dto.getName() != null) company.setName(dto.getName());
        if (dto.getPhone() != null) company.setPhone(dto.getPhone());
        if (dto.getField() != null) company.setField(dto.getField());
        if (dto.getDescription() != null) company.setDescription(dto.getDescription());
        if (dto.getWebsite() != null) company.setWebsite(dto.getWebsite());
        if (dto.getAddress() != null) company.setAddress(dto.getAddress());
        if (dto.getSize() != null) company.setSize(dto.getSize());

        return Optional.of(CompanyDTO.from(companyRepository.save(company)));
    }

    // Upload (or replace) the caller's own company logo. OWNER-only, same authority as editing the
    // profile (the logo is part of the company's public identity). Image content-type validation
    // happens in the controller. A company has at most one logo: a new upload replaces the reference
    // and deletes the previous file so it doesn't orphan on disk. Mirrors CandidateService.uploadPhoto.
    @Transactional
    public Optional<StoredFileDTO> uploadLogo(String ownerEmail, MultipartFile file) {
        User user = userRepository.findByEmail(ownerEmail).orElse(null);
        if (user == null) return Optional.empty();

        CompanyUserProfile actor = companyUserProfileRepository.findByUser(user).orElse(null);
        if (actor == null || actor.getCompanyRole() != CompanyRole.OWNER) return Optional.empty();

        Company company = actor.getCompany();
        String previousLogoId = company.getLogoId();
        StoredFile stored = storedFileService.store(file);
        company.setLogoId(stored.getStorageId());
        companyRepository.save(company);

        if (previousLogoId != null) {
            storedFileService.delete(previousLogoId);
        }
        return Optional.of(StoredFileDTO.from(stored));
    }

    // The company logo bytes, served publicly (the company profile is public). Only publicly
    // visible companies (ACTIVE/PARTNER) qualify, same filter as getVisibleCompanyById. Empty -> 404
    // for every miss — no such company, not publicly visible, or no logo set — so a hidden company
    // can't be probed via its logo.
    @Transactional(readOnly = true)
    public Optional<FileDownload> getVisibleCompanyLogo(Long id) {
        Company company = companyRepository.findById(id)
                .filter(c -> PUBLICLY_VISIBLE.contains(c.getStatus()))
                .orElse(null);
        if (company == null || company.getLogoId() == null) return Optional.empty();

        return storedFileService.load(company.getLogoId());
    }
}
