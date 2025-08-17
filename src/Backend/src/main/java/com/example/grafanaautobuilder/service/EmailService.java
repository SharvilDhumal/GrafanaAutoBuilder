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
                "    <meta name=\"x-apple-disable-message-reformatting\">\n" +
                "    <style>\n" +
                "      a { text-decoration: none; }\n" +
                "      @media (prefers-color-scheme: dark) {\n" +
                "        .card { background:#0b1220 !important; border-color:#1f2937 !important; }\n" +
                "        .muted { color:#9ca3af !important; }\n" +
                "        .bodytxt { color:#e5e7eb !important; }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body style=\"margin:0; padding:0; background-color:#f3f4f6; mso-line-height-rule:exactly; font-family:Arial, Helvetica, sans-serif;\">\n" +
                "    <span style=\"display:none!important; visibility:hidden; opacity:0; color:transparent; height:0; width:0;\">Confirm your email address to activate your account</span>\n" +
                "    <center style=\"width:100%; background:#f3f4f6;\">\n" +
                "      <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; background:#f3f4f6;\">\n" +
                "        <tr>\n" +
                "          <td align=\"center\" style=\"padding:24px 12px;\">\n" +
                "            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; max-width:600px;\">\n" +
                "              <tr>\n" +
                "                <td align=\"center\" style=\"padding:8px 0 16px 0; font-weight:700; font-size:18px; color:#111827;\">\n" +
                "                  <span style=\"color:#2563eb;\">" + brand + "</span>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td class=\"card\" style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:28px;\">\n" +
                "                  <h1 style=\"margin:0 0 12px 0; font-weight:700; font-size:22px; color:#111827;\">Verify your email</h1>\n" +
                "                  <p class=\"bodytxt\" style=\"margin:0 0 18px 0; font-size:14px; color:#374151; line-height:1.6;\">Thanks for signing up. Please confirm your email to finish setting up your account.</p>\n" +
                "                  <!-- Bulletproof button -->\n" +
                "                  <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:18px 0;\">\n" +
                "                    <tr>\n" +
                "                      <td bgcolor=\"#2563eb\" style=\"border-radius:10px;\">\n" +
                "                        <a href=\"" + verificationUrl + "\" style=\"display:inline-block; padding:13px 22px; font-weight:700; font-size:14px; color:#ffffff;\">Verify email</a>\n" +
                "                      </td>\n" +
                "                    </tr>\n" +
                "                  </table>\n" +
                "                  <p class=\"muted\" style=\"margin:0 0 8px 0; font-size:12px; color:#6b7280;\">Having trouble with the button?</p>\n" +
                "                  <a href=\"" + verificationUrl + "\" style=\"font-size:12px; color:#2563eb; text-decoration:underline; word-break:break-all;\">" + verificationUrl + "</a>\n" +
                "                  <hr style=\"border:none; border-top:1px solid #e5e7eb; margin:24px 0;\">\n" +
                "                  <p class=\"muted\" style=\"margin:0; font-size:12px; color:#9ca3af;\">If you didn’t request this, you can safely ignore this email.</p>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td align=\"center\" style=\"padding:16px 0; font-size:11px; color:#9ca3af;\">&copy; " + java.time.Year.now() + " " + brand + ". All rights reserved.</td>\n" +
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
                "    <meta name=\"x-apple-disable-message-reformatting\">\n" +
                "    <style>\n" +
                "      a { text-decoration: none; }\n" +
                "      @media (prefers-color-scheme: dark) {\n" +
                "        .card { background:#0b1220 !important; border-color:#1f2937 !important; }\n" +
                "        .muted { color:#9ca3af !important; }\n" +
                "        .bodytxt { color:#e5e7eb !important; }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body style=\"margin:0; padding:0; background-color:#f3f4f6; mso-line-height-rule:exactly; font-family:Arial, Helvetica, sans-serif;\">\n" +
                "    <span style=\"display:none!important; visibility:hidden; opacity:0; color:transparent; height:0; width:0;\">Reset your password securely</span>\n" +
                "    <center style=\"width:100%; background:#f3f4f6;\">\n" +
                "      <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; background:#f3f4f6;\">\n" +
                "        <tr>\n" +
                "          <td align=\"center\" style=\"padding:24px 12px;\">\n" +
                "            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%; max-width:600px;\">\n" +
                "              <tr>\n" +
                "                <td align=\"center\" style=\"padding:8px 0 16px 0; font-weight:700; font-size:18px; color:#111827;\">\n" +
                "                  <span style=\"color:#2563eb;\">" + brand + "</span>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td class=\"card\" style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:28px;\">\n" +
                "                  <h1 style=\"margin:0 0 12px 0; font-weight:700; font-size:22px; color:#111827;\">Reset your password</h1>\n" +
                "                  <p class=\"bodytxt\" style=\"margin:0 0 18px 0; font-size:14px; color:#374151; line-height:1.6;\">Click the button below to change your password. If you didn’t request this, you can ignore this email.</p>\n" +
                "                  <!-- Bulletproof button -->\n" +
                "                  <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:18px 0;\">\n" +
                "                    <tr>\n" +
                "                      <td bgcolor=\"#2563eb\" style=\"border-radius:10px;\">\n" +
                "                        <a href=\"" + resetUrl + "\" style=\"display:inline-block; padding:13px 22px; font-weight:700; font-size:14px; color:#ffffff;\">Reset password</a>\n" +
                "                      </td>\n" +
                "                    </tr>\n" +
                "                  </table>\n" +
                "                  <p class=\"muted\" style=\"margin:0 0 8px 0; font-size:12px; color:#6b7280;\">Having trouble with the button?</p>\n" +
                "                  <a href=\"" + resetUrl + "\" style=\"font-size:12px; color:#2563eb; text-decoration:underline; word-break:break-all;\">" + resetUrl + "</a>\n" +
                "                  <hr style=\"border:none; border-top:1px solid #e5e7eb; margin:24px 0;\">\n" +
                "                  <p class=\"muted\" style=\"margin:0; font-size:12px; color:#9ca3af;\">If you didn’t request this, you can safely ignore this email.</p>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td align=\"center\" style=\"padding:16px 0; font-size:11px; color:#9ca3af;\">&copy; " + java.time.Year.now() + " " + brand + ". All rights reserved.</td>\n" +
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