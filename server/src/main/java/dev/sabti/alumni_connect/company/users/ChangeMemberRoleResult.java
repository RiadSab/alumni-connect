package dev.sabti.alumni_connect.company.users;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// A multi-outcome result, unlike the Optional "soft failure" used elsewhere — this operation
// has two genuinely different failure HTTP statuses (403 vs 404) that a single empty Optional
// can't carry. The service returns the domain Outcome (staying HTTP-agnostic) and the controller
// maps it to a status; profile is non-null only on SUCCESS.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChangeMemberRoleResult {

    public enum Outcome {
        SUCCESS,     // -> 200 + updated profile
        FORBIDDEN,   // actor isn't the OWNER, targeted themselves, or tried to grant OWNER -> 403
        NOT_FOUND    // target isn't a member of the actor's company (or doesn't exist) -> 404
    }

    private final Outcome outcome;
    private final CompanyUserProfileDTO profile;

    public static ChangeMemberRoleResult success(CompanyUserProfileDTO profile) {
        return new ChangeMemberRoleResult(Outcome.SUCCESS, profile);
    }

    public static ChangeMemberRoleResult forbidden() {
        return new ChangeMemberRoleResult(Outcome.FORBIDDEN, null);
    }

    public static ChangeMemberRoleResult notFound() {
        return new ChangeMemberRoleResult(Outcome.NOT_FOUND, null);
    }
}
