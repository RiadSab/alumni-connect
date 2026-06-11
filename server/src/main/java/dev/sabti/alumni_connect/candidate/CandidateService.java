package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandidateService {
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    // Empty -> 403 if the caller has no CandidateProfile (e.g. a company-user hitting
    // this endpoint), same "soft failure" pattern as JobApplicationService.getMyApplications.
    @Transactional(readOnly = true)
    public Optional<CandidateProfileDTO> getMyProfile(String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> candidateProfileRepository.findByUser(user)
                        .map(profile -> CandidateProfileDTO.from(user, profile)));
    }

    // Admin-only lookup by User id (e.g. reviewing a pending candidate from
    // /api/admin/pending-users). Empty -> 404 if the id doesn't exist or isn't a candidate.
    @Transactional(readOnly = true)
    public Optional<CandidateProfileDTO> getProfileById(Long userId) {
        return userRepository.findById(userId)
                .flatMap(user -> candidateProfileRepository.findByUser(user)
                        .map(profile -> CandidateProfileDTO.from(user, profile)));
    }

    // Partial update across both User (name/phone) and CandidateProfile (everything else).
    // Fields are all nullable: null means "leave unchanged".
    @Transactional
    public Optional<CandidateProfileDTO> updateMyProfile(String email, UpdateCandidateProfileDTO dto) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return Optional.empty();

        CandidateProfile profile = candidateProfileRepository.findByUser(user).orElse(null);
        if (profile == null) return Optional.empty();

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getIsStudent() != null) profile.setIsStudent(dto.getIsStudent());
        if (dto.getStudentId() != null) profile.setStudentId(dto.getStudentId());
        if (dto.getFieldOfStudy() != null) profile.setFieldOfStudy(dto.getFieldOfStudy());
        if (dto.getGraduationYear() != null) profile.setGraduationYear(dto.getGraduationYear());
        if (dto.getDateOfBirth() != null) profile.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getSkills() != null) profile.setSkills(dto.getSkills());
        if (dto.getCurrentJobTitle() != null) profile.setCurrentJobTitle(dto.getCurrentJobTitle());
        if (dto.getCurrentCompany() != null) profile.setCurrentCompany(dto.getCurrentCompany());
        if (dto.getExperienceYears() != null) profile.setExperienceYears(dto.getExperienceYears());
        if (dto.getLinkedinUrl() != null) profile.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getGithubUrl() != null) profile.setGithubUrl(dto.getGithubUrl());
        if (dto.getPortfolioUrl() != null) profile.setPortfolioUrl(dto.getPortfolioUrl());
        if (dto.getBio() != null) profile.setBio(dto.getBio());

        userRepository.save(user);
        return Optional.of(CandidateProfileDTO.from(user, candidateProfileRepository.save(profile)));
    }
}
