package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import dev.sabti.alumni_connect.job.repositories.JobApplicationRepository;
import dev.sabti.alumni_connect.job.repositories.JobOfferRepository;
import dev.sabti.alumni_connect.job.repositories.SavedJobRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobOfferService {
    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;
    private final CandidateProfileRepository candidateProfileRepository;

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
            if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
                spec = spec.and(JobOfferSpecs.titleContains(criteria.getQ()));
            }
            if (criteria.getCity() != null) {
                spec = spec.and(JobOfferSpecs.hasCity(criteria.getCity()));
            }
            if (criteria.getEmploymentType() != null) {
                spec = spec.and(JobOfferSpecs.hasEmploymentType(criteria.getEmploymentType()));
            }
            if (criteria.getIsRemote() != null) {
                spec = spec.and(JobOfferSpecs.isRemote(criteria.getIsRemote()));
            }
            if (criteria.getSkills() != null && !criteria.getSkills().isEmpty()) {
                spec = spec.and(JobOfferSpecs.hasAnySkill(criteria.getSkills()));
            }
        }
        return jobOfferRepository.findAll(spec, pageable).map(JobOfferDTO::from);
    }

    // "Recommended for you": OPEN offers ranked by how many of their required skills overlap
    // with the candidate's own profile skills (system-push, vs the caller-pull of the search
    // above). The endpoint is gated to ROLE CANDIDATE, so the profile is expected to exist; the
    // defensive empties cover the impossible cases the same way without throwing. A candidate
    // with no skills gets an empty page on purpose — that's the signal to go fill their profile,
    // not a reason to fall back to generic listings (which would hide the gap).
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getRecommendedOffers(String candidateEmail, Pageable pageable) {
        User user = userRepository.findByEmail(candidateEmail).orElse(null);
        if (user == null) return Page.empty(pageable);

        CandidateProfile profile = candidateProfileRepository.findByUser(user).orElse(null);
        if (profile == null) return Page.empty(pageable);

        Set<String> skills = profile.getSkills();
        if (skills == null || skills.isEmpty()) return Page.empty(pageable);

        // Lowercased to match the query's lower(s) comparison; skillsRequired isn't normalized on
        // write, so the matching is made case-insensitive here rather than relying on stored case.
        List<String> normalized = skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
        if (normalized.isEmpty()) return Page.empty(pageable);

        return jobOfferRepository.findOpenOffersMatchingSkills(normalized, pageable).map(JobOfferDTO::from);
    }

    // Posting is restricted to a company's own OWNER/RECRUITER, and only while that company is ACTIVE
    // — the same authority-boundary reasoning that shaped the join-flow (registerCompanyMember). The
    // two failures are now separate 403s with their own message: "not a poster role" vs "company not
    // active" (previously both collapsed to one empty 403).
    @Transactional
    public JobOfferDTO postJobOffer(String posterEmail, CreateJobOfferDTO dto) {
        User user = userRepository.findByEmail(posterEmail)
                .orElseThrow(() -> new ForbiddenException("Only a company owner or recruiter can post job offers"));
        CompanyUserProfile profile = companyUserProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Only a company owner or recruiter can post job offers"));
        if (profile.getCompanyRole() != CompanyRole.OWNER && profile.getCompanyRole() != CompanyRole.RECRUITER) {
            throw new ForbiddenException("Only a company owner or recruiter can post job offers");
        }
        if (profile.getCompany().getStatus() != CompanyStatus.ACTIVE) {
            throw new ForbiddenException("Your company is not active and cannot post job offers");
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

        return JobOfferDTO.from(jobOfferRepository.save(offer));
    }

    // OPEN offers are public; anything else (DRAFT/CLOSED/EXPIRED) is visible only to the posting
    // company's OWNER/RECRUITER (404 otherwise, so a draft's existence isn't leaked). hasApplied is
    // filled for a candidate caller.
    @Transactional(readOnly = true)
    public JobOfferDTO getJobOfferById(String callerEmail, Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));

        if (offer.getStatus() != JobStatus.OPEN
                && (callerEmail == null || resolvePosterForCompany(callerEmail, offer.getCompany().getId()).isEmpty())) {
            throw new NotFoundException("Job offer not found");
        }

        JobOfferDTO dto = JobOfferDTO.from(offer);
        CandidateProfile candidate = callerEmail == null ? null
                : userRepository.findByEmail(callerEmail).flatMap(candidateProfileRepository::findByUser).orElse(null);
        dto.setHasApplied(hasApplied(candidate, offer));
        dto.setIsSaved(candidate != null && savedJobRepository.existsByCandidateAndJobOffer(candidate, offer));
        return dto;
    }

    // True if a candidate caller already has an active (non-withdrawn) application to this offer —
    // the same condition apply() enforces, so a withdrawn one reads false and re-applying is allowed.
    private boolean hasApplied(CandidateProfile candidate, JobOffer offer) {
        if (candidate == null) return false;
        return jobApplicationRepository
                .existsByJobOfferAndApplicantAndApplicationStatusNot(offer, candidate, ApplicationStatus.WITHDRAWN);
    }

    // Editing (including closing/reopening via status) is restricted to the posting company's own
    // OWNER/RECRUITER. 404 for both "no such offer" and "not your company's offer" — same
    // 404-for-both, don't-leak-existence reasoning as getJobOfferById above (the old code returned an
    // empty 403 for both; 404 is the consistent choice). Fields are all nullable: null means "leave
    // unchanged", same pattern as ReviewApplicationDTO.
    @Transactional
    public JobOfferDTO updateJobOffer(String callerEmail, Long id, UpdateJobOfferDTO dto) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));

        if (resolvePosterForCompany(callerEmail, offer.getCompany().getId()).isEmpty()) {
            throw new NotFoundException("Job offer not found");
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

        return JobOfferDTO.from(jobOfferRepository.save(offer));
    }

    // All offers of the caller's own company (any status) — viewable by any of its members, so a
    // plain MEMBER can see what the company has posted. Posting/editing stay OWNER/RECRUITER
    // (enforced in postJobOffer/updateJobOffer). 403 if the caller isn't a company user at all.
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getMyCompanyJobOffers(String callerEmail, Pageable pageable) {
        CompanyUserProfile profile = userRepository.findByEmail(callerEmail)
                .flatMap(companyUserProfileRepository::findByUser)
                .orElseThrow(() -> new ForbiddenException("Only a company user can do this"));

        return jobOfferRepository.findByCompany(profile.getCompany(), pageable).map(JobOfferDTO::from);
    }

    // Company dashboard counts via aggregate queries (postings / open / total applicants) —
    // viewable by any company user, same as the offers listing. 403 if not a company user.
    @Transactional(readOnly = true)
    public CompanyOfferStatsDTO getMyCompanyStats(String callerEmail) {
        CompanyUserProfile profile = userRepository.findByEmail(callerEmail)
                .flatMap(companyUserProfileRepository::findByUser)
                .orElseThrow(() -> new ForbiddenException("Only a company user can do this"));

        Company company = profile.getCompany();
        long totalPostings = jobOfferRepository.countByCompany(company);
        long openPostings = jobOfferRepository.countByCompanyAndStatus(company, JobStatus.OPEN);
        long totalApplicants = jobOfferRepository.sumApplicationCountByCompany(company);
        return new CompanyOfferStatsDTO(totalPostings, openPostings, totalApplicants);
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
