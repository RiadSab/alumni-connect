package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.storage.StoredFile;
import dev.sabti.alumni_connect.storage.StoredFileDTO;
import dev.sabti.alumni_connect.storage.StoredFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandidateService {
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final StoredFileService storedFileService;

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

    // Stores the uploaded CV and points the profile at it. Empty -> 403 if the caller isn't a
    // candidate, same as the other /me operations. Content-type/size validation happens in the
    // controller (PDF only) before we get here. A candidate has at most one resume: uploading a
    // new one replaces the reference and deletes the previous file so it doesn't orphan on disk.
    @Transactional
    public Optional<StoredFileDTO> uploadResume(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return Optional.empty();

        CandidateProfile profile = candidateProfileRepository.findByUser(user).orElse(null);
        if (profile == null) return Optional.empty();

        String previousResumeId = profile.getResumeId();
        StoredFile stored = storedFileService.store(file);
        profile.setResumeId(stored.getStorageId());
        candidateProfileRepository.save(profile);

        if (previousResumeId != null) {
            storedFileService.delete(previousResumeId);
        }
        return Optional.of(StoredFileDTO.from(stored));
    }

    // The candidate's own CV bytes for download, as a three-outcome result so the controller can
    // return 403 (not a candidate, consistent with getMyProfile) vs 404 (candidate, no resume) —
    // a distinction a single Optional can't carry. A set resumeId whose file/metadata is somehow
    // gone is treated as NO_RESUME (404), not a 500.
    @Transactional(readOnly = true)
    public ResumeDownload getMyResume(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResumeDownload.notCandidate();

        CandidateProfile profile = candidateProfileRepository.findByUser(user).orElse(null);
        if (profile == null) return ResumeDownload.notCandidate();

        String resumeId = profile.getResumeId();
        if (resumeId == null) return ResumeDownload.noResume();

        return storedFileService.load(resumeId)
                .map(ResumeDownload::found)
                .orElseGet(ResumeDownload::noResume);
    }
}
