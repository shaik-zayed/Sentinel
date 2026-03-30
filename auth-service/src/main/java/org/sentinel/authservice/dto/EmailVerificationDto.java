package org.sentinel.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerificationDto {
    @NotBlank(message = "Token is required")
    private String token;
}