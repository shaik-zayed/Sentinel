package org.sentinel.authservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.email.from}")
    private String fromEmail;

    @Value("${application.email.verification-url}")
    private String verificationUrl;

    @Value("${application.email.password-reset-url}")
    private String passwordResetUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            String verificationLink = verificationUrl + "?token=" + token;

            log.info("Sending verification email to: {}", maskEmail(toEmail));
            log.debug("Verification link: {}", verificationLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address - Sentinel");
            helper.setText(buildVerificationEmailBody(verificationLink), true); // true = HTML

            mailSender.send(message);

            log.info("Verification email sent successfully to: {}", maskEmail(toEmail));

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", maskEmail(toEmail), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = passwordResetUrl + "?token=" + token;

            log.info("Sending password reset email to: {}", maskEmail(toEmail));
            log.debug("Reset link: {}", resetLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password - Sentinel");
            helper.setText(buildPasswordResetEmailBody(resetLink), true);

            mailSender.send(message);

            log.info("Password reset email sent successfully to: {}", maskEmail(toEmail));

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", maskEmail(toEmail), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Async
    public void sendAccountLockedEmail(String toEmail) {
        try {
            log.info("Sending account locked notification to: {}", maskEmail(toEmail));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Account Security Alert - Sentinel");
            helper.setText(buildAccountLockedEmailBody(), true);

            mailSender.send(message);

            log.info("Account locked email sent successfully to: {}", maskEmail(toEmail));

        } catch (MessagingException e) {
            log.error("Failed to send account locked email to: {}", maskEmail(toEmail), e);
        }
    }

    private String buildVerificationEmailBody(String verificationLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            background-color: #f9f9f9;
                        }
                        .content {
                            background-color: white;
                            padding: 30px;
                            border-radius: 8px;
                        }
                        .button {
                            display: inline-block;
                            background-color: #4CAF50;
                            color: white !important;
                            padding: 12px 24px;
                            text-decoration: none;
                            border-radius: 4px;
                            margin: 20px 0;
                        }
                        .footer {
                            margin-top: 20px;
                            font-size: 12px;
                            color: #666;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="content">
                            <h2>Welcome to Sentinel! 🛡️</h2>
                            <p>Thank you for registering. Please verify your email address to activate your account.</p>
                            <p>
                                <a href="%s" class="button">Verify Email Address</a>
                            </p>
                            <p>Or copy this link into your browser:</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                            <p class="footer">
                                This link will expire in 24 hours.<br>
                                If you didn't create an account, please ignore this email.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(verificationLink, verificationLink);
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }
                        .content { background-color: white; padding: 30px; border-radius: 8px; }
                        .button { 
                            display: inline-block; 
                            background-color: #2196F3; 
                            color: white !important; 
                            padding: 12px 24px; 
                            text-decoration: none; 
                            border-radius: 4px; 
                            margin: 20px 0; 
                        }
                        .footer { margin-top: 20px; font-size: 12px; color: #666; }
                        .warning { color: #ff9800; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="content">
                            <h2>Password Reset Request 🔐</h2>
                            <p>We received a request to reset your password for your Sentinel account.</p>
                            <p>
                                <a href="%s" class="button">Reset Password</a>
                            </p>
                            <p>Or copy this link into your browser:</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                            <p class="footer">
                                <span class="warning">⚠️ This link will expire in 1 hour.</span><br><br>
                                If you didn't request a password reset, please ignore this email.<br>
                                Your password will remain unchanged.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetLink, resetLink);
    }

    private String buildAccountLockedEmailBody() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }
                        .content { background-color: white; padding: 30px; border-radius: 8px; }
                        .alert { 
                            background-color: #fff3cd; 
                            border-left: 4px solid #ffc107; 
                            padding: 15px; 
                            margin: 20px 0; 
                        }
                        .footer { margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="content">
                            <h2>⚠️ Account Security Alert</h2>
                            <div class="alert">
                                <p><strong>Your account has been temporarily locked</strong></p>
                                <p>This happened due to multiple failed login attempts.</p>
                            </div>
                            <p>This is a security measure to protect your account from unauthorized access.</p>
                            <p><strong>What happens next?</strong></p>
                            <ul>
                                <li>Your account will be automatically unlocked after 30 minutes</li>
                                <li>Or you can contact support for immediate assistance</li>
                            </ul>
                            <p class="footer">
                                If you didn't attempt to log in, please change your password immediately.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
