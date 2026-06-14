package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFile;
import dev.sabti.alumni_connect.storage.StoredFileDTO;
import dev.sabti.alumni_connect.storage.StoredFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CandidateService {
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final StoredFileService storedFileService;

    // ForbiddenException (403) if the caller isn't a candidate (no CandidateProfile) — e.g. a
    // company-user hitting this endpoint. Same boundary as before, now thrown rather than an
    // Optional.empty mapped in the controller.
    @Transactional(readOnly = true)
    public CandidateProfileDTO getMyProfile(String email) {
        User user = requireUser(email);
        CandidateProfile profile = requireCandidate(user);
        return CandidateProfileDTO.from(user, profile);
    }

    // Admin-only lookup by User id (e.g. reviewing a pending candidate from
    // /api/admin/pending-users). NotFoundException (404) if the id doesn't exist or isn't a candidate.
    @Transactional(readOnly = true)
    public CandidateProfileDTO getProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Candidate not found"));
        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Candidate not found"));
        return CandidateProfileDTO.from(user, profile);
    }

    // Partial update across both User (name/phone) and CandidateProfile (everything else).
    // Fields are all nullable: null means "leave unchanged".
    @Transactional
    public CandidateProfileDTO updateMyProfile(String email, UpdateCandidateProfileDTO dto) {
        User user = requireUser(email);
        CandidateProfile profile = requireCandidate(user);

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
        return CandidateProfileDTO.from(user, candidateProfileRepository.save(profile));
    }

    // Stores the uploaded CV and points the profile at it. ForbiddenException if the caller isn't a
    // candidate. Content-type/size validation happens in the controller (PDF only) before we get
    // here. A candidate has at most one resume: uploading a new one replaces the reference and
    // deletes the previous file so it doesn't orphan on disk.
    @Transactional
    public StoredFileDTO uploadResume(String email, MultipartFile file) {
        User user = requireUser(email);
        CandidateProfile profile = requireCandidate(user);

        String previousResumeId = profile.getResumeId();
        StoredFile stored = storedFileService.store(file);
        profile.setResumeId(stored.getStorageId());
        candidateProfileRepository.save(profile);

        if (previousResumeId != null) {
            storedFileService.delete(previousResumeId);
        }
        return StoredFileDTO.from(stored);
    }

    // The candidate's own CV bytes for download. ForbiddenException (403) if the caller isn't a
    // candidate (consistent with the other /me endpoints); NotFoundException (404) if they are but
    // have no resume. A set resumeId whose file/metadata is somehow gone is treated as 404, not a 500.
    @Transactional(readOnly = true)
    public FileDownload getMyResume(String email) {
        User user = requireUser(email);
        CandidateProfile profile = requireCandidate(user);

        String resumeId = profile.getResumeId();
        if (resumeId == null) throw new NotFoundException("No resume on file");

        return storedFileService.load(resumeId)
                .orElseThrow(() -> new NotFoundException("No resume on file"));
    }

    // Stores the uploaded profile photo and points the profile at it. ForbiddenException if the
    // caller isn't a candidate. Image content-type validation happens in the controller. A candidate
    // has at most one photo: a new upload replaces the reference and deletes the previous file.
    @Transactional
    public StoredFileDTO uploadPhoto(String email, MultipartFile file) {
        User user = requireUser(email);
        CandidateProfile profile = requireCandidate(user);

        String previousPhotoId = profile.getProfilePhotoId();
        StoredFile stored = storedFileService.store(file);
        profile.setProfilePhotoId(stored.getStorageId());
        candidateProfileRepository.save(profile);

        if (previousPhotoId != null) {
            storedFileService.delete(previousPhotoId);
        }
        return StoredFileDTO.from(stored);
    }

    // The candidate's own profile photo bytes for download. ForbiddenException (403) if not a
    // candidate, NotFoundException (404) if they are but have no photo. A set profilePhotoId whose
    // file/metadata is gone is treated as 404, not a 500.
    @Transactional(readOnly = true)
    public FileDownload getMyPhoto(String email) {
        User user = requireUser(email);
        CandidateProfile profile = requireCandidate(user);

        String photoId = profile.getProfilePhotoId();
        if (photoId == null) throw new NotFoundException("No profile photo on file");

        return storedFileService.load(photoId)
                .orElseThrow(() -> new NotFoundException("No profile photo on file"));
    }

    // A candidate operation requires a real user behind the email and a CandidateProfile behind that
    // user; either missing -> 403, since the endpoint doesn't apply to the caller. (The user is
    // always present for an authenticated principal; the reachable case is "authenticated, but not a
    // candidate".)
    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
    }

    private CandidateProfile requireCandidate(User user) {
        return candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
    }
}
