package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyUserService {
    private final UserRepository userRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;

    // Empty -> 403 if the caller has no CompanyUserProfile (e.g. a candidate hitting
    // this endpoint), same "soft failure" pattern as CandidateService.getMyProfile.
    @Transactional(readOnly = true)
    public Optional<CompanyUserProfileDTO> getMyProfile(String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> companyUserProfileRepository.findByUser(user)
                        .map(profile -> CompanyUserProfileDTO.from(user, profile)));
    }

    // Partial update across both User (name/phone) and CompanyUserProfile (position only —
    // companyRole/company are admin/owner-controlled). Fields are all nullable: null
    // means "leave unchanged".
    @Transactional
    public Optional<CompanyUserProfileDTO> updateMyProfile(String email, UpdateCompanyUserProfileDTO dto) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return Optional.empty();

        CompanyUserProfile profile = companyUserProfileRepository.findByUser(user).orElse(null);
        if (profile == null) return Optional.empty();

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getPosition() != null) profile.setPosition(dto.getPosition());

        userRepository.save(user);
        return Optional.of(CompanyUserProfileDTO.from(user, companyUserProfileRepository.save(profile)));
    }
}
