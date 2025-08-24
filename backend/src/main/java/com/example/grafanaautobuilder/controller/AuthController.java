package com.example.grafanaautobuilder.controller;

import com.example.grafanaautobuilder.dto.AuthResponse;
import com.example.grafanaautobuilder.dto.ForgotPasswordRequest;
import com.example.grafanaautobuilder.dto.LoginRequest;
import com.example.grafanaautobuilder.dto.ResetPasswordRequest;
import com.example.grafanaautobuilder.dto.SignupRequest;
import com.example.grafanaautobuilder.exception.EmailAlreadyExistsException;
import com.example.grafanaautobuilder.exception.TokenAlreadyUsedException;
import com.example.grafanaautobuilder.exception.TokenExpiredException;
import com.example.grafanaautobuilder.exception.TokenNotFoundException;
import com.example.grafanaautobuilder.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        log.info("Verifying email with token: {}", token);
        try {
            userService.verifyEmail(token);
            String html = "<!doctype html>\n" +
                    "<html lang='en'>\n" +
                    "<head>\n" +
                    "  <meta charset='utf-8'>\n" +
                    "  <meta name='viewport' content='width=device-width, initial-scale=1'>\n" +
                    "  <title>Email Verified</title>\n" +
                    "  <style>\n" +
                    "    body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,\n" +
                    "         Cantarell,Noto Sans,sans-serif;background:#0b1220;color:#e6e8ec;\n" +
                    "         margin:0;display:flex;align-items:center;justify-content:center;min-height:100vh;}\n" +
                    "    .card{background:#131a2a;border:1px solid #243048;border-radius:12px;\n" +
                    "          padding:28px;max-width:520px;box-shadow:0 10px 30px rgba(0,0,0,.35);}\n" +
                    "    h1{margin:0 0 8px;font-size:24px;color:#7ee787;}\n" +
                    "    p{margin:0 0 6px;line-height:1.6;color:#c9d1d9;}\n" +
                    "    .muted{color:#8b949e;font-size:14px}\n" +
                    "    .ok{display:inline-block;margin-top:14px;padding:10px 14px;border-radius:8px;\n" +
                    "        background:#1f6feb;color:#fff;text-decoration:none;}\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <div class='card'>\n" +
                    "    <h1>Email verified</h1>\n" +
                    "    <p>Your email has been successfully verified.</p>\n" +
                    "    <p class='muted'>You can now return to the app and log in. You may close this tab.</p>\n" +
                    "  </div>\n" +
                    "</body>\n" +
                    "</html>";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(html);
        } catch (TokenNotFoundException e) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(buildErrorHtml("Invalid verification link", "The verification token is invalid. Please request a new verification email."));
        } catch (TokenExpiredException e) {
            return ResponseEntity.status(410)
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(buildErrorHtml("Verification link expired", "Your verification link has expired. Please request a new verification email."));
        } catch (TokenAlreadyUsedException e) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(buildSuccessHtml("Email already verified", "Your email was already verified. You can close this tab."));
        } catch (Exception e) {
            log.error("Unexpected error while verifying token", e);
            return ResponseEntity.status(500)
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(buildErrorHtml("Something went wrong", "An unexpected error occurred. Please try again later."));
        }
    }

    private String buildSuccessHtml(String title, String message) {
        return "<!doctype html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "  <meta charset='utf-8'>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1'>\n" +
                "  <title>" + title + "</title>\n" +
                "  <style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans,sans-serif;background:#0b1220;color:#e6e8ec;margin:0;display:flex;align-items:center;justify-content:center;min-height:100vh}.card{background:#131a2a;border:1px solid #243048;border-radius:12px;padding:28px;max-width:520px;box-shadow:0 10px 30px rgba(0,0,0,.35)}h1{margin:0 0 8px;font-size:24px;color:#7ee787}p{margin:0 0 6px;line-height:1.6;color:#c9d1d9}.muted{color:#8b949e;font-size:14px}</style>\n" +
                "</head>\n" +
                "<body><div class='card'><h1>" + title + "</h1><p>" + message + "</p><p class='muted'>You can now return to the app.</p></div></body></html>";
    }

    private String buildErrorHtml(String title, String message) {
        return "<!doctype html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "  <meta charset='utf-8'>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1'>\n" +
                "  <title>" + title + "</title>\n" +
                "  <style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans,sans-serif;background:#0b1220;color:#e6e8ec;margin:0;display:flex;align-items:center;justify-content:center;min-height:100vh}.card{background:#1f242f;border:1px solid #3a4556;border-radius:12px;padding:28px;max-width:520px;box-shadow:0 10px 30px rgba(0,0,0,.35)}h1{margin:0 0 8px;font-size:24px;color:#ff7b72}p{margin:0 0 6px;line-height:1.6;color:#c9d1d9}.muted{color:#8b949e;font-size:14px}</style>\n" +
                "</head>\n" +
                "<body><div class='card'><h1>" + title + "</h1><p>" + message + "</p><p class='muted'>If the problem persists, request a new link.</p></div></body></html>";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.email());
        try {
            AuthResponse resp = userService.login(loginRequest);
            return ResponseEntity.ok(resp);
        } catch (DisabledException e) {
            log.warn("Login blocked. Email not verified for {}", loginRequest.email());
            return ResponseEntity.status(403).body("Email not verified. Please check your inbox and verify your email.");
        } catch (BadCredentialsException e) {
            log.warn("Bad credentials for {}", loginRequest.email());
            return ResponseEntity.status(401).body("Invalid email or password.");
        } catch (Exception e) {
            log.error("Unexpected error during login for {}", loginRequest.email(), e);
            return ResponseEntity.status(500).body("An error occurred during login. Please try again.");
        }
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

    @GetMapping("/account-enabled")
    public ResponseEntity<Boolean> isAccountEnabled(@RequestParam String email) {
        boolean enabled = userService.isEmailEnabled(email);
        log.info("Account enabled check for {} => {}", email, enabled);
        return ResponseEntity.ok(enabled);
    }
}