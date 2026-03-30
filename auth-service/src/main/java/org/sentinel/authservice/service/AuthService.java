package org.sentinel.authservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.authservice.dto.AuthResponse;
import org.sentinel.authservice.dto.LoginRequest;
import org.sentinel.authservice.dto.RegisterRequest;
import org.sentinel.authservice.dto.UserResponseDto;
import org.sentinel.authservice.exceptions.AccountDisabledException;
import org.sentinel.authservice.exceptions.InvalidTokenException;
import org.sentinel.authservice.exceptions.LogoutException;
import org.sentinel.authservice.model.RefreshToken;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuditService auditService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponseDto registerUser(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Processing registration request for email: {}", maskEmail(request.getEmail()));

        User user = userService.registerUser(request);

        auditService.logAction(
                user.getUserId(),
                "USER_REGISTRATION",
                "New user registered",
                httpRequest
        );

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .locked(user.getLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public AuthResponse authenticateUser(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Authentication attempt for email: {} from IP: {}", maskEmail(email), ipAddress);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            UserDetails principal = (UserDetails) authentication.getPrincipal();
            Objects.requireNonNull(principal, "Authentication principal must not be null");
            User user = userService.findByEmail(principal.getUsername());

            // Reset failed login attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                userService.resetFailedLoginAttempts(user);
            }

            // Log successful authentication
            auditService.logAuthenticationAttempt(
                    user.getUserId(),
                    "LOGIN_SUCCESS",
                    true,
                    ipAddress,
                    userAgent,
                    null
            );

            log.info("User authenticated successfully: {}", user.getUserId());

            // Pass HttpServletRequest for proper token creation
            return generateAuthResponse(user, httpRequest);

        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials for email: {} from IP: {}", maskEmail(email), ipAddress);
            handleFailedLogin(email, ipAddress, userAgent, "Invalid credentials");
            throw new BadCredentialsException("Invalid email or password");

        } catch (DisabledException ex) {
            log.warn("Disabled account login attempt: {} from IP: {}", maskEmail(email), ipAddress);

            try {
                User user = userService.findByEmail(email);
                auditService.logAuthenticationAttempt(
                        user.getUserId(),
                        "LOGIN_FAILED_DISABLED",
                        false,
                        ipAddress,
                        userAgent,
                        "Account not activated"
                );
            } catch (Exception ignored) {
                // User might not exist
            }

            throw new AccountDisabledException("Account is not activated. Please verify your email.");

        } catch (LockedException ex) {
            log.warn("Locked account login attempt: {} from IP: {}", maskEmail(email), ipAddress);

            try {
                User user = userService.findByEmail(email);
                auditService.logAuthenticationAttempt(
                        user.getUserId(),
                        "LOGIN_FAILED_LOCKED",
                        false,
                        ipAddress,
                        userAgent,
                        "Account locked"
                );
            } catch (Exception ignored) {
                // User might not exist
            }

            throw new LockedException("Account is locked due to multiple failed login attempts. " +
                    "Your account will be automatically unlocked in 30 minutes or contact support.");
        }
    }

    private void handleFailedLogin(String email, String ipAddress, String userAgent, String reason) {
        try {
            User user = userService.findByEmail(email);
            userService.incrementFailedLoginAttempts(user);

            auditService.logAuthenticationAttempt(
                    user.getUserId(),
                    "LOGIN_FAILED",
                    false,
                    ipAddress,
                    userAgent,
                    reason
            );

        } catch (Exception e) {
            // User might not exist - don't reveal this information
            log.debug("Failed login for non-existent user from IP: {}", ipAddress);
            auditService.logAuthenticationAttempt(
                    null,
                    "LOGIN_FAILED",
                    false,
                    ipAddress,
                    userAgent,
                    "User not found"
            );
        }
    }

    /**
     * ✅ COMPLETELY REWRITTEN - Now uses database-backed refresh tokens with rotation
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenString, HttpServletRequest httpRequest) {
        log.info("Processing token refresh request from IP: {}", getClientIP(httpRequest));

        if (refreshTokenString == null || refreshTokenString.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        // ✅ Step 1: Validate token type (JWT structure)
        if (!jwtUtil.isRefreshToken(refreshTokenString)) {
            log.warn("Attempted to use access token for refresh from IP: {}", getClientIP(httpRequest));
            throw new InvalidTokenException("Invalid token type. Refresh token required.");
        }

        try {
            // ✅ Step 2: Validate against database and check for security issues
            RefreshToken oldToken = refreshTokenService.validateAndGetRefreshToken(refreshTokenString);
            User user = oldToken.getUser();

            // ✅ Step 3: Additional security checks on user account
            if (!user.isActive()) {
                log.warn("Refresh attempt for inactive user: {}", user.getUserId());
                throw new InvalidTokenException("Account is no longer active");
            }

            if (!user.getEnabled()) {
                log.warn("Refresh attempt for disabled user: {}", user.getUserId());
                throw new InvalidTokenException("Account is not enabled");
            }

            if (user.getLocked()) {
                log.warn("Refresh attempt for locked user: {}", user.getUserId());
                throw new InvalidTokenException("Account is locked");
            }

            // ✅ Step 4: Rotate the refresh token (revoke old, create new in same family)
            String newRefreshTokenString = refreshTokenService.rotateRefreshToken(oldToken, httpRequest);

            // ✅ Step 5: Generate new access token
            UserDetails userDetails = new CustomUserDetails(user);
            String newAccessToken = jwtUtil.generateAccessToken(userDetails);

            // ✅ Step 6: Log the refresh operation
            auditService.logAction(
                    user.getUserId(),
                    "TOKEN_REFRESH",
                    String.format("Token refreshed from IP: %s, Family: %s",
                            getClientIP(httpRequest),
                            oldToken.getTokenFamilyId()),
                    httpRequest
            );

            log.info("Token refreshed successfully for user: {}, family: {}",
                    user.getUserId(), oldToken.getTokenFamilyId());

            // ✅ Step 7: Return new token pair
            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshTokenString)
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .expiresIn(jwtUtil.getJwtExpiration())
                    .tokenType("Bearer")
                    .build();

        } catch (InvalidTokenException e) {
            // ✅ Specific token validation errors - already logged in RefreshTokenService
            throw e;
        } catch (SecurityException e) {
            // ✅ Token reuse detected - entire family already revoked in RefreshTokenService
            log.error("SECURITY ALERT: Token reuse detected from IP: {}", getClientIP(httpRequest));
            throw new InvalidTokenException("Security violation detected. All related sessions have been terminated.");
        } catch (Exception ex) {
            log.error("Error refreshing token from IP: {}: {}", getClientIP(httpRequest), ex.getMessage());
            throw new InvalidTokenException("Token refresh failed: " + ex.getMessage());
        }
    }

    /**
     * Creates database-backed refresh tokens
     */
    private AuthResponse generateAuthResponse(User user, HttpServletRequest httpRequest) {
        UserDetails customUserDetails = new CustomUserDetails(user);

        // Generate access token (JWT only, no database storage needed)
        String accessToken = jwtUtil.generateAccessToken(customUserDetails);

        // Creates refresh token with database backing
        String refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);

        log.debug("Generated token pair for user: {}", user.getUserId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .expiresIn(jwtUtil.getJwtExpiration())
                .tokenType("Bearer")
                .build();
    }

    @Transactional(readOnly = true)
    public void validateToken(String token) {
        // Check if token is blacklisted (for access tokens)
        if (tokenBlacklistService.isTokenBlacklistedByToken(token)) {
            throw new InvalidTokenException("Token has been revoked");
        }

        try {
            String userEmail = jwtUtil.extractUsername(token);
            User user = userService.findByEmail(userEmail);

            if (!user.isActive()) {
                throw new InvalidTokenException("User account is not active");
            }

            if (!user.getEnabled()) {
                throw new InvalidTokenException("User account is not enabled");
            }

            UserDetails userDetails = new CustomUserDetails(user);

            if (!jwtUtil.isTokenValid(token, userDetails)) {
                throw new InvalidTokenException("Invalid token");
            }

            log.debug("Token validated for user: {}", user.getUserId());

        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Token validation failed: {}", ex.getMessage());
            throw new InvalidTokenException("Token validation failed: " + ex.getMessage());
        }
    }

    /**
     * Revokes database refresh tokens
     */
    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletRequest httpRequest) {
        try {
            String userEmail = jwtUtil.extractUsername(accessToken);
            User user = userService.findByEmail(userEmail);

            // Blacklist the access token (prevents use until natural expiration)
            tokenBlacklistService.blacklistToken(accessToken, user.getUserId(), "User logout");

            // Revoke the refresh token in the database (permanent invalidation)
            try {
                refreshTokenService.revokeToken(refreshToken, "User logout");
            } catch (InvalidTokenException e) {
                // Token might already be revoked or not found - log but don't fail logout
                log.warn("Could not revoke refresh token during logout for user {}: {}",
                        user.getUserId(), e.getMessage());
            }

            auditService.logAction(
                    user.getUserId(),
                    "LOGOUT",
                    String.format("User logged out from IP: %s", getClientIP(httpRequest)),
                    httpRequest
            );

            log.info("User logged out successfully: {}", user.getUserId());

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Logout from all devices
     */
    @Transactional
    public void logoutAllDevices(String accessToken, HttpServletRequest httpRequest) {
        try {
            String userEmail = jwtUtil.extractUsername(accessToken);
            User user = userService.findByEmail(userEmail);

            // Blacklist current access token
            tokenBlacklistService.blacklistToken(accessToken, user.getUserId(), "Logout all devices");

            // Revoke ALL refresh tokens for this user
            refreshTokenService.revokeAllUserTokens(user.getUserId(), "User requested logout from all devices");

            auditService.logAction(
                    user.getUserId(),
                    "LOGOUT_ALL_DEVICES",
                    String.format("User logged out from all devices. Initiated from IP: %s", getClientIP(httpRequest)),
                    httpRequest
            );

            log.info("User logged out from all devices: {}", user.getUserId());

        } catch (Exception e) {
            log.error("Error during logout all devices: {}", e.getMessage());
            throw new LogoutException("Logout from all devices failed: " + e.getMessage(), e);
        }
    }

    public String extractUserEmailFromToken(String token) {
        return jwtUtil.extractUsername(token);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "********@" + parts[1];
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}