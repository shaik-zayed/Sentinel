package org.sentinel.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sentinel.authservice.dto.ChangePasswordDto;
import org.sentinel.authservice.dto.PageResponse;
import org.sentinel.authservice.dto.UserResponseDto;
import org.sentinel.authservice.model.User;
import org.sentinel.authservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);

        UserResponseDto dto = UserResponseDto.builder()
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

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordDto dto,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.findByEmail(email);
        userService.changePassword(user.getUserId(), dto);

        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully"
        ));
    }

    @GetMapping("/getAllUsers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (paginated, admin only)")
    public ResponseEntity<PageResponse<UserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserResponseDto> userPage = userService.findAllUsers(pageable);

        PageResponse<UserResponseDto> response = PageResponse.<UserResponseDto>builder()
                .content(userPage.getContent())
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (admin only)")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        User user = userService.findById(id);

        UserResponseDto dto = UserResponseDto.builder()
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

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (soft delete, admin only)")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully"
        ));
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user account (admin only)")
    public ResponseEntity<Map<String, String>> unlockUser(@PathVariable UUID id) {
        userService.unlockUserAccount(id);
        return ResponseEntity.ok(Map.of(
                "message", "User account unlocked successfully"
        ));
    }
}