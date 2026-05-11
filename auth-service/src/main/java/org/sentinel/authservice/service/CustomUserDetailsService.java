package org.sentinel.authservice.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.sentinel.authservice.exception.UserNotFoundException;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with this email : " + email));

        return new CustomUserDetails(user);
    }
}