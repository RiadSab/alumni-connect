package dev.sabti.alumni_connect.auth.entities;

// Discriminates the role of a User now that we use composition (a profile entity per role)
// instead of JPA inheritance — used by MyUserDetailsService to determine roles/authorities.
public enum UserType {
    CANDIDATE,
    COMPANY_USER,
    ADMINISTRATOR
}
