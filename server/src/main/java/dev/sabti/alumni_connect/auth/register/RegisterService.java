package dev.sabti.alumni_connect.auth.register;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.CandidateProfileRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyRepository;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;
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

    // Creates the company's first user (its OWNER) together with the Company itself, atomically —
    // there is no path to a Company existing without an accountable person attached to it.
    @Transactional
    public User registerCompanyOwner(RegisterCompanyDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUserType(UserType.COMPANY_USER);
        user = userRepository.save(user);

        Company company = new Company();
        company.setName(dto.getCompanyName());
        company.setEmail(dto.getCompanyEmail());
        company.setPhone(dto.getCompanyPhone());
        company.setField(dto.getCompanyField());
        company.setDescription(dto.getCompanyDescription());
        company.setWebsite(dto.getCompanyWebsite());
        company.setAddress(dto.getCompanyAddress());
        company.setSize(dto.getCompanySize());
        company = companyRepository.save(company);

        CompanyUserProfile profile = new CompanyUserProfile();
        profile.setUser(user);
        profile.setCompany(company);
        profile.setCompanyRole(CompanyRole.OWNER);
        profile.setPosition(dto.getPosition());
        companyUserProfileRepository.save(profile);

        return user;
    }

    // Joins an existing Company as a MEMBER. Unlike registerCandidate/registerCompanyOwner,
    // this can fail on a business rule that @Valid can't express (the referenced company must
    // exist AND be ACTIVE) — modeled as Optional, the same way LoginService signals soft failures.
    @Transactional
    public Optional<User> registerCompanyMember(RegisterCompanyMemberDTO dto) {
        Company company = companyRepository.findById(dto.getCompanyId()).orElse(null);
        if (company == null || company.getStatus() != CompanyStatus.ACTIVE) {
            return Optional.empty();
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUserType(UserType.COMPANY_USER);
        user = userRepository.save(user);

        CompanyUserProfile profile = new CompanyUserProfile();
        profile.setUser(user);
        profile.setCompany(company);
        profile.setCompanyRole(CompanyRole.MEMBER);
        profile.setPosition(dto.getPosition());
        companyUserProfileRepository.save(profile);

        return Optional.of(user);
    }
}
