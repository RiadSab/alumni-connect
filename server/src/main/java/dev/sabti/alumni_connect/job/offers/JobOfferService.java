package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobOfferService {
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;

    // Only OPEN offers are publicly browsable — DRAFT/CLOSED/EXPIRED must stay invisible
    // to candidates, the same "discoverable set is a filtered subset" reasoning as
    // CompanyService.getActiveCompanies (only ACTIVE companies are joinable/browsable).
    // The OPEN constraint is the non-optional base spec; the caller's optional filters are
    // ANDed onto it, so "no filters" yields exactly the previous unfiltered OPEN list.
    // @Transactional keeps the Hibernate session open while .from() resolves the lazy
    // company/postedBy.user associations, so the DTO mapping doesn't depend on
    // Open-Session-In-View.
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getOpenJobOffers(JobOfferSearchCriteria criteria, Pageable pageable) {
        Specification<JobOffer> spec = JobOfferSpecs.isOpen();
        if (criteria != null) {
            if (criteria.q() != null && !criteria.q().isBlank()) {
                spec = spec.and(JobOfferSpecs.titleContains(criteria.q()));
            }
            if (criteria.city() != null) {
                spec = spec.and(JobOfferSpecs.hasCity(criteria.city()));
            }
            if (criteria.employmentType() != null) {
                spec = spec.and(JobOfferSpecs.hasEmploymentType(criteria.employmentType()));
            }
            if (criteria.isRemote() != null) {
                spec = spec.and(JobOfferSpecs.isRemote(criteria.isRemote()));
            }
            if (criteria.skills() != null && !criteria.skills().isEmpty()) {
                spec = spec.and(JobOfferSpecs.hasAnySkill(criteria.skills()));
            }
        }
        return jobOfferRepository.findAll(spec, pageable).map(JobOfferDTO::from);
    }

    // Posting is restricted to a company's own OWNER/RECRUITER, and only while that
    // company is ACTIVE — the same authority-boundary reasoning that shaped the
    // join-flow (registerCompanyMember). Modeled as Optional, the same "soft failure"
    // pattern, since @Valid can't express "is this caller allowed to act for this company".
    @Transactional
    public Optional<JobOfferDTO> postJobOffer(String posterEmail, CreateJobOfferDTO dto) {
        User user = userRepository.findByEmail(posterEmail).orElse(null);
        if (user == null) return Optional.empty();

        CompanyUserProfile profile = companyUserProfileRepository.findByUser(user).orElse(null);
        if (profile == null
                || (profile.getCompanyRole() != CompanyRole.OWNER && profile.getCompanyRole() != CompanyRole.RECRUITER)
                || profile.getCompany().getStatus() != CompanyStatus.ACTIVE) {
            return Optional.empty();
        }

        JobOffer offer = new JobOffer();
        offer.setTitle(dto.getTitle());
        offer.setDescription(dto.getDescription());
        offer.setRequirements(dto.getRequirements());
        offer.setCompany(profile.getCompany());
        offer.setPostedBy(profile);
        offer.setCity(dto.getCity());
        offer.setMinSalary(dto.getMinSalary());
        offer.setMaxSalary(dto.getMaxSalary());
        offer.setEmploymentType(dto.getEmploymentType());
        offer.setApplicationDeadline(dto.getApplicationDeadline());
        offer.setExperienceYears(dto.getExperienceYears());
        offer.setSkillsRequired(dto.getSkillsRequired());
        offer.setIsRemote(dto.getIsRemote() != null ? dto.getIsRemote() : Boolean.FALSE);
        offer.setIsUrgent(dto.getIsUrgent() != null ? dto.getIsUrgent() : Boolean.FALSE);
        offer.setMaxApplications(dto.getMaxApplications());
        offer.setContactEmail(dto.getContactEmail());

        return Optional.of(JobOfferDTO.from(jobOfferRepository.save(offer)));
    }

    // OPEN offers are publicly visible (same as getOpenJobOffers). Anything else
    // (DRAFT/CLOSED/EXPIRED) is only visible to the posting company's own
    // OWNER/RECRUITER — otherwise the controller maps to 404, deliberately not
    // distinguishing "doesn't exist" from "exists but not yours to see", so a
    // draft posting's existence isn't leaked to outsiders.
    @Transactional(readOnly = true)
    public Optional<JobOfferDTO> getJobOfferById(String callerEmail, Long id) {
        JobOffer offer = jobOfferRepository.findById(id).orElse(null);
        if (offer == null) return Optional.empty();

        if (offer.getStatus() == JobStatus.OPEN) {
            return Optional.of(JobOfferDTO.from(offer));
        }

        if (callerEmail == null || resolvePosterForCompany(callerEmail, offer.getCompany().getId()).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(JobOfferDTO.from(offer));
    }

    // Editing (including closing/reopening via status) is restricted to the posting
    // company's own OWNER/RECRUITER — the old PUT /update had no such check, letting
    // any COMPANYUSER edit any company's offer. Fields are all nullable: null means
    // "leave unchanged", same pattern as ReviewApplicationDTO.
    @Transactional
    public Optional<JobOfferDTO> updateJobOffer(String callerEmail, Long id, UpdateJobOfferDTO dto) {
        JobOffer offer = jobOfferRepository.findById(id).orElse(null);
        if (offer == null) return Optional.empty();

        if (resolvePosterForCompany(callerEmail, offer.getCompany().getId()).isEmpty()) {
            return Optional.empty();
        }

        if (dto.getTitle() != null) offer.setTitle(dto.getTitle());
        if (dto.getDescription() != null) offer.setDescription(dto.getDescription());
        if (dto.getRequirements() != null) offer.setRequirements(dto.getRequirements());
        if (dto.getCity() != null) offer.setCity(dto.getCity());
        if (dto.getMinSalary() != null) offer.setMinSalary(dto.getMinSalary());
        if (dto.getMaxSalary() != null) offer.setMaxSalary(dto.getMaxSalary());
        if (dto.getEmploymentType() != null) offer.setEmploymentType(dto.getEmploymentType());
        if (dto.getApplicationDeadline() != null) offer.setApplicationDeadline(dto.getApplicationDeadline());
        if (dto.getStatus() != null) offer.setStatus(dto.getStatus());
        if (dto.getExperienceYears() != null) offer.setExperienceYears(dto.getExperienceYears());
        if (dto.getSkillsRequired() != null) offer.setSkillsRequired(dto.getSkillsRequired());
        if (dto.getIsRemote() != null) offer.setIsRemote(dto.getIsRemote());
        if (dto.getIsUrgent() != null) offer.setIsUrgent(dto.getIsUrgent());
        if (dto.getMaxApplications() != null) offer.setMaxApplications(dto.getMaxApplications());
        if (dto.getContactEmail() != null) offer.setContactEmail(dto.getContactEmail());

        return Optional.of(JobOfferDTO.from(jobOfferRepository.save(offer)));
    }

    // All offers belonging to the caller's own company (any status), regardless of
    // who posted them — matches the company-wide authority already enforced by
    // resolvePosterForCompany (an OWNER/RECRUITER can edit/review any of their
    // company's offers, not just ones they personally posted), so this listing is
    // the discovery entry point for that same authority.
    @Transactional(readOnly = true)
    public Optional<Page<JobOfferDTO>> getMyCompanyJobOffers(String callerEmail, Pageable pageable) {
        CompanyUserProfile profile = userRepository.findByEmail(callerEmail)
                .flatMap(companyUserProfileRepository::findByUser)
                .filter(p -> p.getCompanyRole() == CompanyRole.OWNER || p.getCompanyRole() == CompanyRole.RECRUITER)
                .orElse(null);
        if (profile == null) return Optional.empty();

        return Optional.of(jobOfferRepository.findByCompany(profile.getCompany(), pageable).map(JobOfferDTO::from));
    }

    // Duplicated from JobApplicationService.resolveReviewerForCompany on purpose —
    // small enough that sharing it isn't worth coupling the two sub-features.
    private Optional<CompanyUserProfile> resolvePosterForCompany(String email, Long companyId) {
        return userRepository.findByEmail(email)
                .flatMap(companyUserProfileRepository::findByUser)
                .filter(profile -> profile.getCompanyRole() == CompanyRole.OWNER || profile.getCompanyRole() == CompanyRole.RECRUITER)
                .filter(profile -> profile.getCompany().getId().equals(companyId));
    }
}
