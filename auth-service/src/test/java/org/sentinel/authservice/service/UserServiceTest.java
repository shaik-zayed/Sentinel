package org.sentinel.authservice.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sentinel.authservice.dto.ChangePasswordDto;
import org.sentinel.authservice.dto.RegisterRequest;
import org.sentinel.authservice.exception.InvalidTokenException;
import org.sentinel.authservice.exception.UserAlreadyExistsException;
import org.sentinel.authservice.model.Role;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("UserServiceTest")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    UserService userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("John", "Doe", "john.doe@example.com", "Password123!");
    }

    @Nested
    @DisplayName("RegisterUser")
    class Register {

        @Test
        @DisplayName("RegisterSuccess_EmailSent")
        void registerUser_success_shouldSaveUserAndSendVerificationEmail() {
            //Given
            given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(registerRequest.getPassword())).willReturn("encoded-password");

            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setUserId(UUID.randomUUID());
                LocalDateTime now = LocalDateTime.now();
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                return user;
            });

            // When
            User result = userService.registerUser(registerRequest);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo(registerRequest.getEmail());
            assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
            assertThat(savedUser.getRole()).isEqualTo(Role.USER);
            assertThat(savedUser.getEnabled()).isFalse();

            assertThat(result.getUserId()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();

            assertThat(savedUser.getEmailVerificationToken()).isNotBlank();
            assertThat(savedUser.getEmailVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());

            // Verify the email service was called with the SAME token saved to the DB
            verify(emailService, times(1))
                    .sendVerificationEmail(eq(registerRequest.getEmail()), eq(savedUser.getEmailVerificationToken()));
        }

        @Test
        @DisplayName("DuplicateEmail_ThrowsException")
        void registerUser_duplicateEmail_throwsException() {
            given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(true);

            assertThatThrownBy(() -> userService.registerUser(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User with this email already exists");

            //As the exception is thrown we are checking these two methods never called.
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("Email Verification Success")
        void VerificationSuccess() {
            String token = "randomToken";
            User user = User.builder()
                    .emailVerificationToken(token)
                    .emailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            given(userRepository.findByEmailVerificationToken(token)).willReturn(Optional.of(user));
            userService.verifyEmail(token);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEnabled()).isTrue();
            assertThat(savedUser.getEmailVerificationToken()).isNull();
            assertThat(savedUser.getEmailVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("Invalid Token")
        void InvalidToken() {
            when(userRepository.findByEmailVerificationToken("bad"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.verifyEmail("bad"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("passwordReset")
    class PasswordReset {

        @Test
        @DisplayName("Should generate reset token and set expiry to 1 hour ahead")
        void initiate_success() {
            User user = User.builder().email("test@mail.com").build();

            given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

            userService.initiatePasswordReset(user.getEmail());

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getPasswordResetToken()).isNotNull();
            assertThat(savedUser.getPasswordResetTokenExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(59))
                    .isBefore(LocalDateTime.now().plusHours(1).plusMinutes(1));
        }

        @Test
        @DisplayName("Password reset Success")
        void reset_success() {
            String token = "token";
            User user = User.builder()
                    .passwordResetToken(token)
                    .passwordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            userService.resetPassword(token, "NewPassword@123");

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded");
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("Password change success")
        void success() {
            UUID id = UUID.randomUUID();
            ChangePasswordDto dto = new ChangePasswordDto();
            dto.setCurrentPassword("old");
            dto.setNewPassword("new");

            User user = User.builder().userId(id).password("encoded-old").build();

            given(userRepository.findById(id)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("old", "encoded-old")).willReturn(true);
            given(passwordEncoder.encode("new")).willReturn("encoded-new");

            userService.changePassword(id, dto);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-new");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("User deleted successfully")
        void success() {
            UUID id = UUID.randomUUID();
            User user = User.builder().userId(id).build();

            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.deleteUser(id);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("accountLocking")
    class AccountLocking {

        @Test
        @DisplayName("Should lock user after reaching failed login threshold")
        void shouldLockAfterThreshold() {
            User user = User.builder()
                    .failedLoginAttempts(4)
                    .locked(false)
                    .build();

            userService.incrementFailedLoginAttempts(user);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(savedUser.getLocked()).isTrue();
        }
    }
}