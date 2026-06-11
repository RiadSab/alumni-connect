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
    // @Transactional keeps the Hibernate session open while .from() resolves the lazy
    // company/postedBy.user associations, so the DTO mapping doesn't depend on
    // Open-Session-In-View.
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getOpenJobOffers(Pageable pageable) {
        return jobOfferRepository.findByStatus(JobStatus.OPEN, pageable).map(JobOfferDTO::from);
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
}
