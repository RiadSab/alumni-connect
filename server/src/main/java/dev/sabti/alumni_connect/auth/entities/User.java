package dev.sabti.alumni_connect.auth.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Holds only auth/identity/account-lifecycle data. Role-specific data (Candidate, CompanyUser,
// Administrator) lives in its own profile entity related back to User — composition instead of
// JPA inheritance, so each role's required fields can have real NOT NULL constraints.
@Entity
@Table(name = "users")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    // Never serialized in API responses — only ever written internally
    // (registration/password-change), and read for authentication.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus = UserStatus.PENDING;

    private Boolean emailVerified = false;
    private String preferredLanguage = "en";

    private String statusChangeReason; // nullable, to store reason if rejected/suspended
}
