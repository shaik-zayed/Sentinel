package org.sentinel.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.authservice.dto.ChangePasswordDto;
import org.sentinel.authservice.dto.RegisterRequest;
import org.sentinel.authservice.dto.UserResponseDto;
import org.sentinel.authservice.exceptions.EmailAlreadyVerifiedException;
import org.sentinel.authservice.exceptions.InvalidTokenException;
import org.sentinel.authservice.exceptions.UserAlreadyExistsException;
import org.sentinel.authservice.exceptions.UserNotFoundException;
import org.sentinel.authservice.model.Role;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email");
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        String verificationToken = generateSecureToken();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(false) // Require email verification
                .locked(false)
                .failedLoginAttempts(0)
                .emailVerificationToken(verificationToken)
                .emailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered with ID: {}", savedUser.getUserId());

        // Send verification email asynchronously
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        return savedUser;
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Invalid or expired token");
        }

        user.markEmailAsVerified();
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getUserId());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = findByEmail(email);

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email already verified");
        }

        String newToken = generateSecureToken();
        user.setEmailVerificationToken(newToken);
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), newToken);
        log.info("Verification email resent for user: {}", user.getUserId());
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        try {
            User user = findByEmail(email);

            String resetToken = generateSecureToken();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Password reset initiated for user: {}", user.getUserId());

        } catch (UserNotFoundException e) {
            // Don't reveal if user exists or not
            log.debug("Password reset requested for non-existent email");
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        user.resetFailedLoginAttempts();
        user.setLocked(false);
        userRepository.save(user);

        log.info("Password reset completed for user: {}", user.getUserId());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    public User findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("User not found with email");
                    return new UserNotFoundException("User not found");
                });
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public Page<UserResponseDto> findAllUsers(Pageable pageable) {
        return userRepository.findAllActiveUsers(pageable)
                .map(this::toDto);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Delete attempt for non-existent user: {}", id);
                    return new UserNotFoundException("User not found");
                });

        if (user.getDeletedAt() != null) {
            log.warn("Attempted to delete already deleted user: {}", id);
            throw new IllegalStateException("User already deleted");
        }

        user.softDelete();
        userRepository.save(user);

        log.info("User soft deleted: {}", id);
    }

    @Transactional
    public void incrementFailedLoginAttempts(User user) {
        user.incrementFailedLoginAttempts();
        userRepository.save(user);

        if (user.getLocked()) {
            emailService.sendAccountLockedEmail(user.getEmail());
        }

        log.info("Failed login attempts for user {}: {}",
                user.getUserId(), user.getFailedLoginAttempts());
    }

    @Transactional
    public void resetFailedLoginAttempts(User user) {
        user.resetFailedLoginAttempts();
        user.setLastSuccessfulLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.debug("Reset failed login attempts for user: {}", user.getUserId());
    }

    @Transactional
    public void unlockUserAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setLocked(false);
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        log.info("User account unlocked: {}", userId);
    }

    @Scheduled(cron = "0 0 * * * ?") // Run every hour
    @Transactional
    public void autoUnlockAccounts() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        var lockedUsers = userRepository.findLockedUsersOlderThan(thirtyMinutesAgo);

        for (User user : lockedUsers) {
            user.setLocked(false);
            user.resetFailedLoginAttempts();
            userRepository.save(user);
            log.info("Auto-unlocked account: {}", user.getUserId());
        }
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .locked(user.getLocked())
                .createdAt(user.getCreatedAt())
                .lastSuccessfulLoginAt(user.getLastSuccessfulLoginAt())
                .build();
    }
}