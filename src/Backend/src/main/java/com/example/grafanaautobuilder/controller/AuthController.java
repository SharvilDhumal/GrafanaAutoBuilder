package com.example.grafanaautobuilder.controller;

import com.example.grafanaautobuilder.dto.*;
import com.example.grafanaautobuilder.exception.EmailAlreadyExistsException;
import com.example.grafanaautobuilder.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Received signup request for email: {}", signupRequest.email());
        try {
            userService.signup(signupRequest);
            log.info("Successfully processed signup for email: {}", signupRequest.email());
            return ResponseEntity.ok().build();
        } catch (EmailAlreadyExistsException e) {
            log.warn("Email already exists: {}", signupRequest.email());
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing signup for email: {}", signupRequest.email(), e);
            return ResponseEntity.status(500).body("An error occurred during signup");
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        log.info("Verifying email with token: {}", token);
        userService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.email());
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Forgot password request for email: {}", forgotPasswordRequest.email());
        userService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        log.info("Password reset requested for email: {}", resetPasswordRequest.email());
        userService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Test endpoint called");
        return ResponseEntity.ok("Backend is accessible!");
    }
}