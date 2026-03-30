package org.sentinel.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class UserResponseDto {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Boolean enabled;
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime lastSuccessfulLoginAt;
}