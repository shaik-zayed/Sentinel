package org.sentinel.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.authservice.dto.*;
import org.sentinel.authservice.service.AuthService;
import org.sentinel.authservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        UserResponseDto response = authService.registerUser(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.authenticateUser(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token", description = "Rotates the refresh token and provides a new access token. " + "Old refresh token is automatically revoked.")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader, HttpServletRequest httpRequest) {

        try {
            String refreshToken = extractTokenFromHeader(authHeader);
            AuthResponse response = authService.refreshAccessToken(refreshToken, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed from IP {}: {}",
                    getClientIP(httpRequest), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<Map<String, String>> validateToken(@RequestHeader("Authorization") String authHeader) {

        String token = extractTokenFromHeader(authHeader);
        authService.validateToken(token);
        return ResponseEntity.ok(Map.of(
                "status", "valid",
                "message", "Token is valid"
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate tokens", description = "Blacklists the access token and revokes the refresh token")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader, @RequestBody LogoutRequest logoutRequest, HttpServletRequest httpRequest) {

        try {
            String accessToken = extractTokenFromHeader(authHeader);
            authService.logout(accessToken, logoutRequest.getRefreshToken(), httpRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "timestamp", java.time.Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Logout from all devices
     */
    @PostMapping("/logout-all-devices")
    @Operation(summary = "Logout from all devices", description = "Revokes all refresh tokens for the user. " + "The user will need to login again on all devices.")
    public ResponseEntity<Map<String, String>> logoutAllDevices(@RequestHeader("Authorization") String authHeader, HttpServletRequest httpRequest) {

        try {
            String accessToken = extractTokenFromHeader(authHeader);
            authService.logoutAllDevices(accessToken, httpRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Logged out from all devices successfully. Please login again.",
                    "timestamp", java.time.Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Logout all devices failed: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email address")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody EmailVerificationDto dto) {

        userService.verifyEmail(dto.getToken());
        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully"
        ));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam String email) {

        userService.resendVerificationEmail(email);
        return ResponseEntity.ok(Map.of(
                "message", "Verification email sent"
        ));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody PasswordResetRequestDto dto) {

        userService.initiatePasswordReset(dto.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "If the email exists, a password reset link has been sent"
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetConfirmDto dto) {

        userService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "message", "Password reset successful. Please login with your new password."
        ));
    }

    /**
     * ✅ IMPROVED: Extract token with better error handling
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header format. Expected 'Bearer <token>'");
        }

        String token = authHeader.substring(7);
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token is required in authorization header");
        }

        return token;
    }

    /**
     * Helper method for consistent IP extraction
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}