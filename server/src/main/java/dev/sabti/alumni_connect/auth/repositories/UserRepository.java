package dev.sabti.alumni_connect.auth.repositories;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// JpaSpecificationExecutor backs the admin user-browse (optional status/type filters via
// findAll(Specification, Pageable)); the derived methods stay for the fixed lookups.
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Page<User> findByUserStatus(UserStatus userStatus, Pageable pageable);
    long countByUserStatus(UserStatus userStatus);
}
