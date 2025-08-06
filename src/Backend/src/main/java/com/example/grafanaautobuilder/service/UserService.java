package com.example.grafanaautobuilder.service;

import com.example.grafanaautobuilder.dto.*;
import com.example.grafanaautobuilder.entity.*;
import com.example.grafanaautobuilder.exception.*;
import com.example.grafanaautobuilder.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
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
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public void signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.email())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        User user = new User();
        user.setEmail(signupRequest.email().toLowerCase().trim());
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email().toLowerCase().trim(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtService.generateToken(userDetails);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);

        return new AuthResponse(jwt, expiresAt);
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