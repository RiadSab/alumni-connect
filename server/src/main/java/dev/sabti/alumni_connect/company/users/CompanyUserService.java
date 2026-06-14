package dev.sabti.alumni_connect.company.users;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.company.entities.CompanyRole;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.company.repositories.CompanyUserProfileRepository;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyUserService {
    private final UserRepository userRepository;
    private final CompanyUserProfileRepository companyUserProfileRepository;

    // ForbiddenException (403) if the caller has no CompanyUserProfile (e.g. a candidate hitting this
    // endpoint), same boundary as CandidateService.getMyProfile.
    @Transactional(readOnly = true)
    public CompanyUserProfileDTO getMyProfile(String email) {
        User user = requireUser(email);
        CompanyUserProfile profile = requireCompanyUser(user);
        return CompanyUserProfileDTO.from(user, profile);
    }

    // The caller's own company roster — every member of the company the caller belongs to. Scoped by
    // identity, not by a path id, so no one can list another company's members. ForbiddenException
    // (403) if the caller isn't a company user (e.g. a candidate). Mapping each member needs its
    // User, resolved lazily within this read-only transaction.
    @Transactional(readOnly = true)
    public Page<CompanyUserProfileDTO> getMyCompanyMembers(String email, Pageable pageable) {
        User user = requireUser(email);
        CompanyUserProfile profile = requireCompanyUser(user);

        return companyUserProfileRepository
                .findByCompany(profile.getCompany(), pageable)
                .map(member -> CompanyUserProfileDTO.from(member.getUser(), member));
    }

    // Admin-only lookup by User id (e.g. reviewing a pending company-user from
    // /api/admin/pending-users). NotFoundException (404) if the id doesn't exist or isn't a company user.
    @Transactional(readOnly = true)
    public CompanyUserProfileDTO getProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Company user not found"));
        CompanyUserProfile profile = companyUserProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Company user not found"));
        return CompanyUserProfileDTO.from(user, profile);
    }

    // Partial update across both User (name/phone) and CompanyUserProfile (position only —
    // companyRole/company are admin/owner-controlled). Fields are all nullable: null
    // means "leave unchanged".
    @Transactional
    public CompanyUserProfileDTO updateMyProfile(String email, UpdateCompanyUserProfileDTO dto) {
        User user = requireUser(email);
        CompanyUserProfile profile = requireCompanyUser(user);

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getPosition() != null) profile.setPosition(dto.getPosition());

        userRepository.save(user);
        return CompanyUserProfileDTO.from(user, companyUserProfileRepository.save(profile));
    }

    // v1: a company's OWNER sets another member's role to MEMBER or RECRUITER, within their own
    // company. The guards encode the rules agreed for v1 (single-owner model), each now a distinct
    // exception carrying its own caller-facing reason (previously all 403s collapsed to one empty
    // body via ChangeMemberRoleResult):
    //   - only an OWNER may act                          -> 403
    //   - granting OWNER is ownership transfer, separate -> 403
    //   - an OWNER can't change their own role (would    -> 403
    //     risk leaving the company with no owner)
    //   - target must be a member of the actor's company -> 404 (404-for-both, so other companies'
    //                                                       membership isn't probeable)
    @Transactional
    public CompanyUserProfileDTO changeMemberRole(String actorEmail, Long targetUserId, CompanyRole newRole) {
        User actorUser = requireUser(actorEmail);
        CompanyUserProfile actor = companyUserProfileRepository.findByUser(actorUser)
                .orElseThrow(() -> new ForbiddenException("Only the company owner can change member roles"));
        if (actor.getCompanyRole() != CompanyRole.OWNER) {
            log.warn("Change member role denied: {} is not an OWNER", actorEmail);
            throw new ForbiddenException("Only the company owner can change member roles");
        }
        if (newRole == CompanyRole.OWNER) {
            log.warn("Change member role denied: {} attempted to grant OWNER (ownership transfer is separate)", actorEmail);
            throw new ForbiddenException("Granting the OWNER role is not supported here");
        }
        if (targetUserId.equals(actorUser.getId())) {
            log.warn("Change member role denied: OWNER {} attempted to change their own role", actorEmail);
            throw new ForbiddenException("You cannot change your own role");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        CompanyUserProfile target = companyUserProfileRepository.findByUser(targetUser)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (!target.getCompany().getId().equals(actor.getCompany().getId())) {
            throw new NotFoundException("Member not found");
        }

        target.setCompanyRole(newRole);
        companyUserProfileRepository.save(target);
        return CompanyUserProfileDTO.from(targetUser, target);
    }

    // The /me endpoints require a real user behind the email and a CompanyUserProfile behind that
    // user; either missing -> 403, since the endpoint doesn't apply to the caller. (The user is
    // always present for an authenticated principal; the reachable case is "authenticated, but not a
    // company user".)
    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Not a company user"));
    }

    private CompanyUserProfile requireCompanyUser(User user) {
        return companyUserProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Not a company user"));
    }
}
