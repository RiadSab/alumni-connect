package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserType;
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

    // Public browse: OPEN offers only, with the caller's optional filters ANDed onto the base spec.
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

    // CANDIDATE — OPEN offers ranked by skill overlap with the candidate's profile; empty if no skills.
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getRecommendedOffers(String candidateEmail, Pageable pageable) {
        User user = userRepository.findByEmail(candidateEmail).orElse(null);
        if (user == null) return Page.empty(pageable);

        CandidateProfile profile = candidateProfileRepository.findByUser(user).orElse(null);
        if (profile == null) return Page.empty(pageable);

        Set<String> skills = profile.getSkills();
        if (skills == null || skills.isEmpty()) return Page.empty(pageable);

        List<String> normalized = skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
        if (normalized.isEmpty()) return Page.empty(pageable);

        return jobOfferRepository.findOpenOffersMatchingSkills(normalized, pageable).map(JobOfferDTO::from);
    }

    // Post a job offer — the company's own OWNER/RECRUITER only, and only while the company is ACTIVE.
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

    // Read one offer: OPEN is public; otherwise only the posting company or an applicant (never a DRAFT).
    @Transactional(readOnly = true)
    public JobOfferDTO getJobOfferById(String callerEmail, Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job offer not found"));

        User caller = callerEmail == null ? null : userRepository.findByEmail(callerEmail).orElse(null);
        UserType callerType = caller == null ? null : caller.getUserType();

        CandidateProfile candidate = null;
        boolean isPoster = false;
        if (callerType == UserType.CANDIDATE) {
            candidate = candidateProfileRepository.findByUser(caller).orElse(null);
        } else if (callerType == UserType.COMPANY_USER && offer.getStatus() != JobStatus.OPEN) {
            isPoster = resolvePosterForCompany(callerEmail, offer.getCompany().getId()).isPresent();
        }
        boolean applied = hasApplied(candidate, offer);

        boolean applicantMayView = applied && offer.getStatus() != JobStatus.DRAFT;
        if (offer.getStatus() != JobStatus.OPEN && !isPoster && !applicantMayView) {
            throw new NotFoundException("Job offer not found");
        }

        JobOfferDTO dto = JobOfferDTO.from(offer);
        dto.setHasApplied(applied);
        dto.setIsSaved(candidate != null && savedJobRepository.existsByCandidateAndJobOffer(candidate, offer));
        return dto;
    }

    // True if the candidate has an active (non-withdrawn) application to this offer.
    private boolean hasApplied(CandidateProfile candidate, JobOffer offer) {
        if (candidate == null) return false;
        return jobApplicationRepository
                .existsByJobOfferAndApplicantAndApplicationStatusNot(offer, candidate, ApplicationStatus.WITHDRAWN);
    }

    // Edit (incl. status) — posting company's OWNER/RECRUITER only; null fields left unchanged; 404 otherwise.
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

    // The caller's own company's offers, any status — any company user; 403 otherwise.
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getMyCompanyJobOffers(String callerEmail, Pageable pageable) {
        CompanyUserProfile profile = userRepository.findByEmail(callerEmail)
                .flatMap(companyUserProfileRepository::findByUser)
                .orElseThrow(() -> new ForbiddenException("Only a company user can do this"));

        return jobOfferRepository.findByCompany(profile.getCompany(), pageable).map(JobOfferDTO::from);
    }

    // Company dashboard counts (postings / open / applicants) — any company user; 403 otherwise.
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

    // The caller as an OWNER/RECRUITER of the given company, if they are one.
    private Optional<CompanyUserProfile> resolvePosterForCompany(String email, Long companyId) {
        return userRepository.findByEmail(email)
                .flatMap(companyUserProfileRepository::findByUser)
                .filter(profile -> profile.getCompanyRole() == CompanyRole.OWNER || profile.getCompanyRole() == CompanyRole.RECRUITER)
                .filter(profile -> profile.getCompany().getId().equals(companyId));
    }
}
