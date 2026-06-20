package dev.sabti.alumni_connect.security;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = user.getUserType().name(); // CANDIDATE, COMPANY_USER, or ADMINISTRATOR

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(role)
                .accountExpired(false)
                .accountLocked(user.getUserStatus() == UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(user.getUserStatus() != UserStatus.ACTIVE && user.getUserStatus() != UserStatus.PENDING)
                .build();
    }

}
