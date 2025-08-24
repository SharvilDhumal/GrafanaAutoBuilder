package com.example.grafanaautobuilder.service;

import com.example.grafanaautobuilder.config.JwtService;
import com.example.grafanaautobuilder.dto.AuthResponse;
import com.example.grafanaautobuilder.dto.ForgotPasswordRequest;
import com.example.grafanaautobuilder.dto.LoginRequest;
import com.example.grafanaautobuilder.dto.ResetPasswordRequest;
import com.example.grafanaautobuilder.dto.SignupRequest;
import com.example.grafanaautobuilder.entity.PasswordResetToken;
import com.example.grafanaautobuilder.entity.User;
import com.example.grafanaautobuilder.entity.VerificationToken;
import com.example.grafanaautobuilder.exception.EmailAlreadyExistsException;
import com.example.grafanaautobuilder.exception.TokenAlreadyUsedException;
import com.example.grafanaautobuilder.exception.TokenExpiredException;
import com.example.grafanaautobuilder.exception.TokenNotFoundException;
import com.example.grafanaautobuilder.exception.UserNotFoundException;
import com.example.grafanaautobuilder.repository.PasswordResetTokenRepository;
import com.example.grafanaautobuilder.repository.UserRepository;
import com.example.grafanaautobuilder.repository.VerificationTokenRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, 
                      VerificationTokenRepository verificationTokenRepository,
                      PasswordResetTokenRepository passwordResetTokenRepository,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + normalizedEmail));

        if (user.isEnabled()) {
            return; // no-op if already verified
        }

        // Invalidate previous tokens for this user (optional but cleaner)
        verificationTokenRepository.findAll().stream()
                .filter(vt -> vt.getUser().getId().equals(user.getId()) && !vt.isUsed())
                .forEach(vt -> { vt.setUsed(true); verificationTokenRepository.save(vt); });

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(OffsetDateTime.now().plus(24, ChronoUnit.HOURS));
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public void signup(SignupRequest signupRequest) {
        String normalizedEmail = signupRequest.email().toLowerCase().trim();

        // Check if a user already exists with this normalized email
        var existingOpt = userRepository.findByEmail(normalizedEmail);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (existing.isEnabled()) {
                throw new EmailAlreadyExistsException("Email already in use");
            }

            // If the user exists but is not yet verified, resend a fresh verification token
            resendVerification(normalizedEmail);
            throw new EmailAlreadyExistsException("Email already registered but not verified. We have sent a new verification email.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(signupRequest.password()));
        user.setEnabled(false);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(OffsetDateTime.now().plus(24, ChronoUnit.HOURS));
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new TokenAlreadyUsedException("Verification token already used");
        }

        if (verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new TokenExpiredException("Verification token expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String email = loginRequest.email().toLowerCase().trim();
        String password = loginRequest.password();
        
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new RuntimeException("User account is not verified");
        }
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate JWT token
        String jwt = jwtService.generateToken(user);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);

        return new AuthResponse(jwt, expiresAt);
    }

    public boolean isEmailEnabled(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .map(User::isEnabled)
                .orElse(false);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        User user = userRepository.findByEmail(forgotPasswordRequest.email().toLowerCase().trim())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + forgotPasswordRequest.email()));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(OffsetDateTime.now().plus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(resetPasswordRequest.token())
                .orElseThrow(() -> new TokenNotFoundException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new TokenAlreadyUsedException("Password reset token already used");
        }

        if (resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new TokenExpiredException("Password reset token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(resetPasswordRequest.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}