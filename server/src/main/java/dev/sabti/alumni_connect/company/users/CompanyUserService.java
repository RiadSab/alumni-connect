package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
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

    // Admin-only lookup by User id (e.g. reviewing a pending company-user from
    // /api/admin/pending-users). Empty -> 404 if the id doesn't exist or isn't a company user.
    @Transactional(readOnly = true)
    public Optional<CompanyUserProfileDTO> getProfileById(Long userId) {
        return userRepository.findById(userId)
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

    // v1: a company's OWNER sets another member's role to MEMBER or RECRUITER, within their own
    // company. The guards encode the rules agreed for v1 (single-owner model):
    //   - only an OWNER may act                       -> FORBIDDEN
    //   - granting OWNER is ownership transfer, not a  -> FORBIDDEN
    //     role edit, and out of scope here
    //   - an OWNER can't change their own role (would  -> FORBIDDEN
    //     risk leaving the company with no owner)
    //   - target must be a member of the actor's       -> NOT_FOUND
    //     company (404-for-both, so other companies'
    //     membership isn't probeable)
    @Transactional
    public ChangeMemberRoleResult changeMemberRole(String actorEmail, Long targetUserId, CompanyRole newRole) {
        User actorUser = userRepository.findByEmail(actorEmail).orElse(null);
        if (actorUser == null) return ChangeMemberRoleResult.forbidden();

        CompanyUserProfile actor = companyUserProfileRepository.findByUser(actorUser).orElse(null);
        if (actor == null || actor.getCompanyRole() != CompanyRole.OWNER) {
            log.warn("Change member role denied: {} is not an OWNER", actorEmail);
            return ChangeMemberRoleResult.forbidden();
        }
        if (newRole == CompanyRole.OWNER) {
            log.warn("Change member role denied: {} attempted to grant OWNER (ownership transfer is separate)", actorEmail);
            return ChangeMemberRoleResult.forbidden();
        }
        if (targetUserId.equals(actorUser.getId())) {
            log.warn("Change member role denied: OWNER {} attempted to change their own role", actorEmail);
            return ChangeMemberRoleResult.forbidden();
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return ChangeMemberRoleResult.notFound();

        CompanyUserProfile target = companyUserProfileRepository.findByUser(targetUser).orElse(null);
        if (target == null || !target.getCompany().getId().equals(actor.getCompany().getId())) {
            return ChangeMemberRoleResult.notFound();
        }

        target.setCompanyRole(newRole);
        companyUserProfileRepository.save(target);
        return ChangeMemberRoleResult.success(CompanyUserProfileDTO.from(targetUser, target));
    }
}
