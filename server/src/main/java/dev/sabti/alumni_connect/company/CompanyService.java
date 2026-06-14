package dev.sabti.alumni_connect.company;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
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
    // NotFoundException (404) for both "no such id" and "exists but not publicly visible" — a caller
    // must not be able to tell a suspended/rejected company apart from one that never existed.
    @Transactional(readOnly = true)
    public CompanyDTO getVisibleCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .filter(c -> PUBLICLY_VISIBLE.contains(c.getStatus()))
                .orElseThrow(() -> new NotFoundException("Company not found"));
        return CompanyDTO.from(company);
    }

    // A company OWNER edits their own company's public profile — until now a company was write-once
    // (create at registration, then frozen), unlike candidates who already have PATCH /me. Scoped to
    // the caller's own company (resolved from their CompanyUserProfile), so there's no path id to
    // reconcile. OWNER-only: a RECRUITER manages offers/applications, not the company's identity, so
    // a non-owner gets 403. Fields are all nullable: null means "leave unchanged".
    @Transactional
    public CompanyDTO updateMyCompany(String ownerEmail, UpdateCompanyDTO dto) {
        Company company = requireOwner(ownerEmail).getCompany();

        if (dto.getName() != null) company.setName(dto.getName());
        if (dto.getPhone() != null) company.setPhone(dto.getPhone());
        if (dto.getField() != null) company.setField(dto.getField());
        if (dto.getDescription() != null) company.setDescription(dto.getDescription());
        if (dto.getWebsite() != null) company.setWebsite(dto.getWebsite());
        if (dto.getAddress() != null) company.setAddress(dto.getAddress());
        if (dto.getSize() != null) company.setSize(dto.getSize());

        return CompanyDTO.from(companyRepository.save(company));
    }

    // Upload (or replace) the caller's own company logo. OWNER-only (403 otherwise), same authority
    // as editing the profile (the logo is part of the company's public identity). Image content-type
    // validation happens in the controller. A company has at most one logo: a new upload replaces the
    // reference and deletes the previous file so it doesn't orphan on disk.
    @Transactional
    public StoredFileDTO uploadLogo(String ownerEmail, MultipartFile file) {
        Company company = requireOwner(ownerEmail).getCompany();

        String previousLogoId = company.getLogoId();
        StoredFile stored = storedFileService.store(file);
        company.setLogoId(stored.getStorageId());
        companyRepository.save(company);

        if (previousLogoId != null) {
            storedFileService.delete(previousLogoId);
        }
        return StoredFileDTO.from(stored);
    }

    // The company logo bytes, served publicly (the company profile is public). Only publicly visible
    // companies (ACTIVE/PARTNER) qualify, same filter as getVisibleCompanyById. NotFoundException
    // (404) for every miss — no such company, not publicly visible, or no logo set — so a hidden
    // company can't be probed via its logo.
    @Transactional(readOnly = true)
    public FileDownload getVisibleCompanyLogo(Long id) {
        Company company = companyRepository.findById(id)
                .filter(c -> PUBLICLY_VISIBLE.contains(c.getStatus()))
                .orElseThrow(() -> new NotFoundException("Company logo not found"));
        if (company.getLogoId() == null) throw new NotFoundException("Company logo not found");

        return storedFileService.load(company.getLogoId())
                .orElseThrow(() -> new NotFoundException("Company logo not found"));
    }

    // The endpoints above that mutate a company require the caller to be that company's OWNER. A user
    // missing entirely, with no CompanyUserProfile, or not an OWNER -> 403, since the operation
    // doesn't apply to them. (The user is always present for an authenticated principal; the reachable
    // cases are "authenticated but not a company user" and "a company user who isn't the owner".)
    private CompanyUserProfile requireOwner(String ownerEmail) {
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ForbiddenException("Only the company owner can do this"));
        CompanyUserProfile actor = companyUserProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Only the company owner can do this"));
        if (actor.getCompanyRole() != CompanyRole.OWNER) {
            throw new ForbiddenException("Only the company owner can do this");
        }
        return actor;
    }
}
