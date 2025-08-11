package com.example.grafanaautobuilder.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

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
        String subject = "Verify your email for Grafana AutoBuilder";
        String html = buildVerificationHtml(verificationUrl);
        // Try HTML email first; fall back to text if something goes wrong
        try {
            sendEmailHtml(toEmail, subject, html);
        } catch (Exception ex) {
            String text = "Please click the following link to verify your email:\n" + verificationUrl;
            sendEmail(toEmail, subject, text);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        String subject = "Password reset for Grafana AutoBuilder";
        String html = buildResetHtml(resetUrl);
        try {
            sendEmailHtml(toEmail, subject, html);
        } catch (Exception ex) {
            String text = "Please click the following link to reset your password:\n" + resetUrl;
            sendEmail(toEmail, subject, text);
        }
    }

    private void sendEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            // Log the error but don't fail the signup process
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            // For development, we'll just log the verification URL
            if (subject.contains("Verify")) {
                String verificationUrl = backendBaseUrl + "/api/auth/verify?token=" + text.substring(text.lastIndexOf("=") + 1);
                System.out.println("Verification URL for " + toEmail + ": " + verificationUrl);
            }
        }
    }

    private void sendEmailHtml(String toEmail, String subject, String htmlContent) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true enables HTML
        mailSender.send(mimeMessage);
    }

    private String buildVerificationHtml(String verificationUrl) {
        String brand = "Grafana AutoBuilder";
        return "<!doctype html>\n" +
                "<html lang=\"en\" style=\"margin:0; padding:0;\">\n" +
                "  <head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <title>Verify your email</title>\n" +
                "  </head>\n" +
                "  <body style=\"margin:0; padding:0; background-color:#f3f4f6; mso-line-height-rule:exactly;\">\n" +
                "    <center style=\"width:100%; background:#f3f4f6;\">\n" +
                "      <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; background:#f3f4f6;\">\n" +
                "        <tr>\n" +
                "          <td align=\"center\" style=\"padding:24px 12px;\">\n" +
                "            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; max-width:600px;\">\n" +
                "              <tr>\n" +
                "                <td style=\"padding:8px 0 16px 0; font:700 18px Arial, Helvetica, sans-serif; color:#111827;\">\n" +
                "                  <span style=\"color:#2563eb;\">" + brand + "</span>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:24px;\">\n" +
                "                  <h1 style=\"margin:0 0 12px 0; font:600 20px Arial, Helvetica, sans-serif; color:#111827;\">Verify your email</h1>\n" +
                "                  <p style=\"margin:0 0 16px 0; font:14px Arial, Helvetica, sans-serif; color:#374151; line-height:1.6;\">Thanks for signing up. Please confirm your email to finish setting up your account.</p>\n" +
                "                  <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:16px 0;\">\n" +
                "                    <tr>\n" +
                "                      <td align=\"center\" bgcolor=\"#2563eb\" style=\"border-radius:8px;\">\n" +
                "                        <a href=\"" + verificationUrl + "\" style=\"display:inline-block; padding:12px 20px; font:700 14px Arial, Helvetica, sans-serif; color:#ffffff; text-decoration:none;\">Verify email</a>\n" +
                "                      </td>\n" +
                "                    </tr>\n" +
                "                  </table>\n" +
                "                  <p style=\"margin:0 0 8px 0; font:12px Arial, Helvetica, sans-serif; color:#6b7280;\">Having trouble with the button?</p>\n" +
                "                  <a href=\"" + verificationUrl + "\" style=\"font:12px Arial, Helvetica, sans-serif; color:#2563eb; text-decoration:underline; word-break:break-all;\">" + verificationUrl + "</a>\n" +
                "                  <hr style=\"border:none; border-top:1px solid #e5e7eb; margin:24px 0;\">\n" +
                "                  <p style=\"margin:0; font:12px Arial, Helvetica, sans-serif; color:#9ca3af;\">If you didn’t request this, you can safely ignore this email.</p>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td align=\"center\" style=\"padding:16px 0; font:11px Arial, Helvetica, sans-serif; color:#9ca3af;\">&copy; " + java.time.Year.now() + " " + brand + ". All rights reserved.</td>\n" +
                "              </tr>\n" +
                "            </table>\n" +
                "          </td>\n" +
                "        </tr>\n" +
                "      </table>\n" +
                "    </center>\n" +
                "  </body>\n" +
                "</html>\n";
    }

    private String buildResetHtml(String resetUrl) {
        String brand = "Grafana AutoBuilder";
        return "<!doctype html>\n" +
                "<html lang=\"en\" style=\"margin:0; padding:0;\">\n" +
                "  <head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <title>Password reset</title>\n" +
                "  </head>\n" +
                "  <body style=\"margin:0; padding:0; background-color:#f3f4f6; mso-line-height-rule:exactly;\">\n" +
                "    <center style=\"width:100%; background:#f3f4f6;\">\n" +
                "      <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; background:#f3f4f6;\">\n" +
                "        <tr>\n" +
                "          <td align=\"center\" style=\"padding:24px 12px;\">\n" +
                "            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; max-width:600px;\">\n" +
                "              <tr>\n" +
                "                <td style=\"padding:8px 0 16px 0; font:700 18px Arial, Helvetica, sans-serif; color:#111827;\">\n" +
                "                  <span style=\"color:#2563eb;\">" + brand + "</span>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:24px;\">\n" +
                "                  <h1 style=\"margin:0 0 12px 0; font:600 20px Arial, Helvetica, sans-serif; color:#111827;\">Reset your password</h1>\n" +
                "                  <p style=\"margin:0 0 16px 0; font:14px Arial, Helvetica, sans-serif; color:#374151; line-height:1.6;\">Click the button below to change your password. If you didn’t request this, you can ignore this email.</p>\n" +
                "                  <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:16px 0;\">\n" +
                "                    <tr>\n" +
                "                      <td align=\"center\" bgcolor=\"#2563eb\" style=\"border-radius:8px;\">\n" +
                "                        <a href=\"" + resetUrl + "\" style=\"display:inline-block; padding:12px 20px; font:700 14px Arial, Helvetica, sans-serif; color:#ffffff; text-decoration:none;\">Reset password</a>\n" +
                "                      </td>\n" +
                "                    </tr>\n" +
                "                  </table>\n" +
                "                  <p style=\"margin:0 0 8px 0; font:12px Arial, Helvetica, sans-serif; color:#6b7280;\">Having trouble with the button?</p>\n" +
                "                  <a href=\"" + resetUrl + "\" style=\"font:12px Arial, Helvetica, sans-serif; color:#2563eb; text-decoration:underline; word-break:break-all;\">" + resetUrl + "</a>\n" +
                "                  <hr style=\"border:none; border-top:1px solid #e5e7eb; margin:24px 0;\">\n" +
                "                  <p style=\"margin:0; font:12px Arial, Helvetica, sans-serif; color:#9ca3af;\">If you didn’t request this, you can safely ignore this email.</p>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td align=\"center\" style=\"padding:16px 0; font:11px Arial, Helvetica, sans-serif; color:#9ca3af;\">&copy; " + java.time.Year.now() + " " + brand + ". All rights reserved.</td>\n" +
                "              </tr>\n" +
                "            </table>\n" +
                "          </td>\n" +
                "        </tr>\n" +
                "      </table>\n" +
                "    </center>\n" +
                "  </body>\n" +
                "</html>\n";
    }
}