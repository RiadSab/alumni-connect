package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import org.springframework.data.jpa.domain.Specification;

// Predicate builders for the admin user-browse, ANDed onto an always-true base.
final class UserSpecs {
    private UserSpecs() {}

    static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> cb.equal(root.get("userStatus"), status);
    }

    static Specification<User> hasType(UserType type) {
        return (root, query, cb) -> cb.equal(root.get("userType"), type);
    }
}
