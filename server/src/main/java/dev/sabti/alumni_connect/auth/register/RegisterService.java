package dev.sabti.alumni_connect.auth.register;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerCandidate(RegisterCandidateDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUserType(UserType.CANDIDATE);
        user = userRepository.save(user);

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(user);
        profile.setIsStudent(dto.getIsStudent());
        profile.setStudentId(dto.getStudentId());
        profile.setFieldOfStudy(dto.getFieldOfStudy());
        profile.setGraduationYear(dto.getGraduationYear());
        profile.setSkills(dto.getSkills());
        profile.setCurrentJobTitle(dto.getCurrentJobTitle());
        profile.setCurrentCompany(dto.getCurrentCompany());
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setLinkedinUrl(dto.getLinkedinUrl());
        profile.setGithubUrl(dto.getGithubUrl());
        profile.setPortfolioUrl(dto.getPortfolioUrl());
        profile.setBio(dto.getBio());
        candidateProfileRepository.save(profile);

        return user;
    }
}
