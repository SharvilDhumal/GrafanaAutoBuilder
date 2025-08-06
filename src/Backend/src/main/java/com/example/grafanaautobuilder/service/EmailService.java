package com.example.grafanaautobuilder.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.frontendBaseUrl}")
    private String frontendBaseUrl;

    @Value("${app.backendBaseUrl}")
    private String backendBaseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = backendBaseUrl + "/api/auth/verify?token=" + token;
        String subject = "Verify your email for Grafana Autobuilder";
        String text = "Please click the following link to verify your email:\n" + verificationUrl;

        sendEmail(toEmail, subject, text);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        String subject = "Password reset for Grafana Autobuilder";
        String text = "Please click the following link to reset your password:\n" + resetUrl;

        sendEmail(toEmail, subject, text);
    }

    private void sendEmail(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}