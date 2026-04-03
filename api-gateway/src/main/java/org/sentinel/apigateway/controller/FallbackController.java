package org.sentinel.apigateway.controller;

import org.sentinel.apigateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public ResponseEntity<ErrorResponse> authServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "Auth Service is temporarily unavailable. Please try again later.",
                        503,
                        System.currentTimeMillis()
                ));
    }

    @RequestMapping("/fallback/user")
    public ResponseEntity<ErrorResponse> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "User Service is temporarily unavailable. Please try again later.",
                        503,
                        System.currentTimeMillis()
                ));
    }
}