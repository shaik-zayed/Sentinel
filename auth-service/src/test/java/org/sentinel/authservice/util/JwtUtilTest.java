package org.sentinel.authservice.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sentinel.authservice.model.Role;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.service.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil")
class JwtUtilTest {
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString(
                    "this-is-my-super-secret-key-of-32-byte-test-secret!!".getBytes());

    private static final long ACCESS_EXPIRY = 900_000L; //15 mins
    private static final long REFRESH_EXPIRY = 604_800_000L; //7 days

    private JwtUtil jwtUtil;
    private CustomUserDetails userDetails;
    private User user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);

        user = User.builder()
                .userId(UUID.randomUUID())
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.USER)
                .enabled(true)
                .locked(false)
                .build();

        userDetails = new CustomUserDetails(user);
    }

    // -------------------------------------------------------------------------
    // Access token generation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("access token generation")
    class AccessTokenGeneration {

        @Test
        void generatesNonNullToken() {
            assertThat(jwtUtil.generateAccessToken(userDetails)).isNotNull();
        }

        @Test
        void subjectIsUserEmail() {
            String token = jwtUtil.generateAccessToken(userDetails);

            assertThat(jwtUtil.extractUsername(token))
                    .isEqualTo("alice@example.com");
        }

        @Test
        void tokenTypeClaimIsAccess() {
            String token = jwtUtil.generateAccessToken(userDetails);

            assertThat(jwtUtil.isRefreshToken(token)).isFalse();
        }

        @Test
        void authoritiesClaimPresent() {
            String token = jwtUtil.generateAccessToken(userDetails);

            List<String> authorities = jwtUtil.extractClaim(token,
                    c -> c.get("authorities", List.class));
            assertThat(authorities).containsExactly("ROLE_USER");
        }

        @Test
        void userIdClaimPresent() {
            String token = jwtUtil.generateAccessToken(userDetails);

            String userId = jwtUtil.extractClaim(token,
                    c -> c.get("userId", String.class));
            assertThat(userId).isEqualTo(user.getUserId().toString());
        }

        @Test
        void jtiClaimIsUniquePerToken() {
            String t1 = jwtUtil.generateAccessToken(userDetails);
            String t2 = jwtUtil.generateAccessToken(userDetails);

            String jti1 = jwtUtil.extractClaim(t1, Claims::getId);
            String jti2 = jwtUtil.extractClaim(t2, Claims::getId);

            assertThat(jti1).isNotEqualTo(jti2);
        }

        @Test
        void expirationIsApproximatelyAccessExpiry() {
            long before = System.currentTimeMillis();
            String token = jwtUtil.generateAccessToken(userDetails);
            long after = System.currentTimeMillis();

            Instant exp = jwtUtil.extractExpirationAsInstantTime(token);

            assertThat(exp.toEpochMilli())
                    .isBetween(before + ACCESS_EXPIRY - 1000,
                            after + ACCESS_EXPIRY + 1000);
        }
    }
    // -------------------------------------------------------------------------
    // Refresh token generation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("refresh token generation")
    class RefreshTokenGeneration {

        @Test
        void tokenTypeClaimIsRefresh() {
            String token = jwtUtil.generateRefreshToken(userDetails);

            assertThat(jwtUtil.isRefreshToken(token)).isTrue();
        }

        @Test
        void subjectIsUserEmail() {
            String token = jwtUtil.generateRefreshToken(userDetails);

            assertThat(jwtUtil.extractUsername(token))
                    .isEqualTo("alice@example.com");
        }

        @Test
        void expirationIsApproximatelyRefreshExpiry() {
            long before = System.currentTimeMillis();
            String token = jwtUtil.generateRefreshToken(userDetails);
            long after = System.currentTimeMillis();

            Instant exp = jwtUtil.extractExpirationAsInstantTime(token);

            assertThat(exp.toEpochMilli())
                    .isBetween(before + REFRESH_EXPIRY - 1000,
                            after + REFRESH_EXPIRY + 1000);
        }
    }

    // -------------------------------------------------------------------------
    // Token validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("token validation")
    class TokenValidation {

        @Test
        void validAccessTokenPassesValidation() {
            String token = jwtUtil.generateAccessToken(userDetails);

            assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        void wrongUserFails() {
            String token = jwtUtil.generateAccessToken(userDetails);

            UserDetails otherUser = new CustomUserDetails(
                    User.builder()
                            .userId(UUID.randomUUID())
                            .email("bob@example.com")
                            .role(Role.USER)
                            .enabled(true)
                            .locked(false)
                            .build());

            assertThat(jwtUtil.isTokenValid(token, otherUser)).isFalse();
        }

        @Test
        void tamperedTokenFails() {
            String token = jwtUtil.generateAccessToken(userDetails);
            String tampered = token.substring(0, token.length() - 4) + "XXXX";

            assertThat(jwtUtil.isTokenValid(tampered, userDetails)).isFalse();
        }

        @Test
        void expiredTokenFails() throws Exception {
            // Generate a token that expires in 1ms
            JwtUtil shortLived = new JwtUtil(TEST_SECRET, 1L, REFRESH_EXPIRY);
            String token = shortLived.generateAccessToken(userDetails);
            Thread.sleep(10); // let it expire

            assertThat(shortLived.isTokenValid(token, userDetails)).isFalse();
        }

        @Test
        void nullTokenReturnsFalse() {
            assertThat(jwtUtil.isTokenValid(null, userDetails)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Secret key validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("constructor validates secret key")
    class SecretKeyValidation {

        @Test
        void tooShortKeyThrows() {
            String shortKey = Base64.getEncoder().encodeToString(
                    "short".getBytes()); // < 32 bytes

            assertThatThrownBy(() -> new JwtUtil(shortKey, ACCESS_EXPIRY, REFRESH_EXPIRY))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void nullKeyThrows() {
            assertThatThrownBy(() -> new JwtUtil(null, ACCESS_EXPIRY, REFRESH_EXPIRY))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void blankKeyThrows() {
            assertThatThrownBy(() -> new JwtUtil("", ACCESS_EXPIRY, REFRESH_EXPIRY))
                    .isInstanceOf(Exception.class);
        }
    }

    // -------------------------------------------------------------------------
    // Admin role
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("admin user gets ROLE_ADMIN in authorities claim")
    void adminRoleInClaims() {
        User adminUser = User.builder()
                .userId(UUID.randomUUID())
                .email("admin@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .locked(false)
                .build();

        String token = jwtUtil.generateAccessToken(new CustomUserDetails(adminUser));

        List<String> authorities = jwtUtil.extractClaim(token,
                c -> c.get("authorities", List.class));
        assertThat(authorities).containsExactly("ROLE_ADMIN");
    }
}