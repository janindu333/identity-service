package com.baber.identityservice.service;

import com.baber.identityservice.config.ServiceLogger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending email notifications
 * Supports SMTP via MailHog or other SMTP servers for testing/development
 */
@Service
public class EmailNotificationService {
    private final ServiceLogger logger = new ServiceLogger(EmailNotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.from:noreply@baberbooking.com}")
    private String fromEmail;

    @Value("${app.email.use-smtp:true}")
    private boolean useSmtp;

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink, 
                                      int expirationMinutes, String ipAddress) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would send password reset email to: " + toEmail);
            logger.info("Reset Link: " + resetLink);
            return;
        }

        logger.info("Sending password reset email to: " + toEmail);
        logger.info("Reset Link: " + resetLink);
        logger.info("Token expires in: " + expirationMinutes + " minutes");
        logger.info("Requested from IP: " + ipAddress);

        // Try to send via SMTP if enabled and mailSender is available
        if (useSmtp && mailSender != null) {
            try {
                sendEmailViaSmtp(toEmail, userName, resetLink, expirationMinutes, ipAddress, true);
                logger.info("Password reset email sent successfully via SMTP to: " + toEmail);
                return;
            } catch (Exception e) {
                logger.error("Failed to send email via SMTP: " + e.getMessage());
                logger.info("Falling back to log-only mode");
            }
        }

        // Fallback: Log email details (for testing when SMTP is not configured)
        logger.info("========================================");
        logger.info("PASSWORD RESET EMAIL (Log Mode)");
        logger.info("========================================");
        logger.info("To: " + toEmail);
        logger.info("Subject: Reset Your Password");
        logger.info("Reset Link: " + resetLink);
        logger.info("Token expires in: " + expirationMinutes + " minutes");
        logger.info("========================================");
    }

    /**
     * Send email verification email
     */
    public void sendVerificationEmail(String toEmail, String userName, String verificationLink, 
                                      int expirationMinutes) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would send verification email to: " + toEmail);
            logger.info("Verification Link: " + verificationLink);
            return;
        }

        logger.info("Sending email verification email to: " + toEmail);
        logger.info("Verification Link: " + verificationLink);
        logger.info("Token expires in: " + expirationMinutes + " minutes");

        // Try to send via SMTP if enabled and mailSender is available
        if (useSmtp && mailSender != null) {
            try {
                sendVerificationEmailViaSmtp(toEmail, userName, verificationLink, expirationMinutes);
                logger.info("Verification email sent successfully via SMTP to: " + toEmail);
                return;
            } catch (Exception e) {
                logger.error("Failed to send verification email via SMTP: " + e.getMessage());
                logger.info("Falling back to log-only mode");
            }
        }

        // Fallback: Log email details
        logger.info("========================================");
        logger.info("EMAIL VERIFICATION EMAIL (Log Mode)");
        logger.info("========================================");
        logger.info("To: " + toEmail);
        logger.info("Subject: Verify Your Email Address");
        logger.info("Verification Link: " + verificationLink);
        logger.info("Token expires in: " + expirationMinutes + " minutes");
        logger.info("========================================");
    }

    /**
     * Send password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would send password reset confirmation to: " + toEmail);
            return;
        }

        logger.info("Sending password reset confirmation email to: " + toEmail);

        // Try to send via SMTP if enabled and mailSender is available
        if (useSmtp && mailSender != null) {
            try {
                sendEmailViaSmtp(toEmail, userName, null, 0, null, false);
                logger.info("Password reset confirmation email sent successfully via SMTP to: " + toEmail);
                return;
            } catch (Exception e) {
                logger.error("Failed to send confirmation email via SMTP: " + e.getMessage());
                logger.info("Falling back to log-only mode");
            }
        }

        // Fallback: Log email details
        logger.info("========================================");
        logger.info("PASSWORD RESET CONFIRMATION EMAIL (Log Mode)");
        logger.info("========================================");
        logger.info("To: " + toEmail);
        logger.info("Subject: Your Password Has Been Reset");
        logger.info("========================================");
    }

    /**
     * Send email via SMTP (MailHog or other SMTP server)
     */
    private void sendEmailViaSmtp(String toEmail, String userName, String resetLink,
                                  int expirationMinutes, String ipAddress, boolean isResetEmail) 
            throws MessagingException {
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail != null ? fromEmail : "noreply@baberbooking.com");
        helper.setTo(toEmail);

        if (isResetEmail) {
            // Password reset email
            helper.setSubject("Reset Your Password - Baber Booking");
            
            String htmlContent = buildPasswordResetEmailHtml(
                userName != null ? userName : "User", 
                resetLink != null ? resetLink : "", 
                expirationMinutes, 
                ipAddress
            );
            helper.setText(htmlContent, true); // true = HTML content
            
        } else {
            // Password reset confirmation email
            helper.setSubject("Your Password Has Been Reset - Baber Booking");
            
            String htmlContent = buildPasswordResetConfirmationEmailHtml(
                userName != null ? userName : "User"
            );
            helper.setText(htmlContent, true);
        }

        mailSender.send(message);
    }

    /**
     * Send verification email via SMTP
     */
    private void sendVerificationEmailViaSmtp(String toEmail, String userName, String verificationLink,
                                              int expirationMinutes) throws MessagingException {
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail != null ? fromEmail : "noreply@baberbooking.com");
        helper.setTo(toEmail);
        helper.setSubject("Verify Your Email Address - Baber Booking");
        
        String htmlContent = buildVerificationEmailHtml(
            userName != null ? userName : "User", 
            verificationLink != null ? verificationLink : "", 
            expirationMinutes
        );
        helper.setText(htmlContent, true); // true = HTML content

        mailSender.send(message);
    }

    /**
     * Build HTML email template for email verification
     */
    private String buildVerificationEmailHtml(String userName, String verificationLink, int expirationMinutes) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #fce4ec 0%%, #ffffff 100%%);
                        padding: 40px 20px;
                        line-height: 1.6;
                    }
                    .email-container { 
                        max-width: 600px; 
                        margin: 0 auto;
                        background: #ffffff;
                        border-radius: 16px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .branding-section {
                        background: linear-gradient(135deg, #2d2d2d 0%%, #1a1a1a 100%%);
                        padding: 40px 30px;
                        color: #ffffff;
                    }
                    .brand-badge {
                        display: inline-block;
                        background: #ec4899;
                        color: white;
                        padding: 8px 16px;
                        border-radius: 8px;
                        font-weight: bold;
                        font-size: 18px;
                        letter-spacing: 1px;
                        margin-bottom: 12px;
                    }
                    .brand-subtitle {
                        color: #a0a0a0;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 24px;
                    }
                    .content-section {
                        padding: 40px 30px;
                        background: #ffffff;
                    }
                    .welcome-text {
                        color: #6b7280;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 8px;
                    }
                    .title {
                        color: #111827;
                        font-size: 28px;
                        font-weight: bold;
                        margin-bottom: 12px;
                    }
                    .instruction {
                        color: #6b7280;
                        font-size: 14px;
                        margin-bottom: 32px;
                    }
                    .message {
                        color: #374151;
                        font-size: 16px;
                        margin-bottom: 24px;
                    }
                    .message strong {
                        color: #111827;
                    }
                    .button-container {
                        text-align: center;
                        margin: 32px 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 14px 32px;
                        background: #8b5cf6;
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 16px;
                        transition: background 0.3s ease;
                    }
                    .button:hover {
                        background: #7c3aed;
                    }
                    .link-text {
                        color: #6b7280;
                        font-size: 14px;
                        margin: 24px 0;
                        word-break: break-all;
                    }
                    .link-url {
                        color: #3b82f6;
                        text-decoration: none;
                    }
                    .link-url:hover {
                        text-decoration: underline;
                    }
                    .expiry-notice {
                        background: #fef3c7;
                        border-left: 4px solid #f59e0b;
                        padding: 12px 16px;
                        border-radius: 4px;
                        margin: 24px 0;
                    }
                    .expiry-notice strong {
                        color: #92400e;
                    }
                    .success-box {
                        background: #d1fae5;
                        border-left: 4px solid #10b981;
                        padding: 16px;
                        border-radius: 8px;
                        margin: 24px 0;
                        text-align: center;
                    }
                    .success-box p {
                        color: #065f46;
                        font-size: 16px;
                        font-weight: 600;
                        margin: 0;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 24px 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    .footer-text {
                        color: #6b7280;
                        font-size: 12px;
                    }
                    .footer-brand {
                        color: #111827;
                        font-weight: 600;
                    }
                    @media only screen and (max-width: 600px) {
                        body { padding: 20px 10px; }
                        .branding-section, .content-section, .footer {
                            padding: 24px 20px;
                        }
                        .title {
                            font-size: 24px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="branding-section">
                        <div class="brand-badge">SALOONY</div>
                        <div class="brand-subtitle">SALON SOLUTION</div>
                    </div>
                    <div class="content-section">
                        <div class="welcome-text">Email Verification</div>
                        <h1 class="title">Welcome to Saloony!</h1>
                        <p class="instruction">Please verify your email address to complete your registration.</p>
                        
                        <p class="message">Hello <strong>%s</strong>,</p>
                        <p class="message">Thank you for registering with Saloony! Please click the button below to verify your email address:</p>
                        
                        <div class="button-container">
                            <a href="%s" class="button">Verify Email Address</a>
                        </div>
                        
                        <p class="link-text">Or copy and paste this link into your browser:<br>
                            <a href="%s" class="link-url">%s</a>
                        </p>
                        
                        <div class="expiry-notice">
                            <strong>⏰ This link will expire in %d minutes.</strong>
                        </div>
                        
                        <div class="success-box">
                            <p>✅ Once verified, you'll be able to access all features!</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p class="footer-text">
                            Best regards,<br>
                            <span class="footer-brand">Saloony Team</span>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """;
        
        return String.format(template, 
            userName != null ? userName : "User", 
            verificationLink != null ? verificationLink : "", 
            verificationLink != null ? verificationLink : "", 
            verificationLink != null ? verificationLink : "", 
            expirationMinutes);
    }

    /**
     * Build HTML email template for password reset
     */
    private String buildPasswordResetEmailHtml(String userName, String resetLink, 
                                              int expirationMinutes, String ipAddress) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #fce4ec 0%%, #ffffff 100%%);
                        padding: 40px 20px;
                        line-height: 1.6;
                    }
                    .email-container { 
                        max-width: 600px; 
                        margin: 0 auto;
                        background: #ffffff;
                        border-radius: 16px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .branding-section {
                        background: linear-gradient(135deg, #2d2d2d 0%%, #1a1a1a 100%%);
                        padding: 40px 30px;
                        color: #ffffff;
                    }
                    .brand-badge {
                        display: inline-block;
                        background: #ec4899;
                        color: white;
                        padding: 8px 16px;
                        border-radius: 8px;
                        font-weight: bold;
                        font-size: 18px;
                        letter-spacing: 1px;
                        margin-bottom: 12px;
                    }
                    .brand-subtitle {
                        color: #a0a0a0;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 24px;
                    }
                    .content-section {
                        padding: 40px 30px;
                        background: #ffffff;
                    }
                    .welcome-text {
                        color: #6b7280;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 8px;
                    }
                    .title {
                        color: #111827;
                        font-size: 28px;
                        font-weight: bold;
                        margin-bottom: 12px;
                    }
                    .instruction {
                        color: #6b7280;
                        font-size: 14px;
                        margin-bottom: 32px;
                    }
                    .message {
                        color: #374151;
                        font-size: 16px;
                        margin-bottom: 24px;
                    }
                    .message strong {
                        color: #111827;
                    }
                    .button-container {
                        text-align: center;
                        margin: 32px 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 14px 32px;
                        background: #8b5cf6;
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 16px;
                        transition: background 0.3s ease;
                    }
                    .button:hover {
                        background: #7c3aed;
                    }
                    .link-text {
                        color: #6b7280;
                        font-size: 14px;
                        margin: 24px 0;
                        word-break: break-all;
                    }
                    .link-url {
                        color: #3b82f6;
                        text-decoration: none;
                    }
                    .link-url:hover {
                        text-decoration: underline;
                    }
                    .expiry-notice {
                        background: #fef3c7;
                        border-left: 4px solid #f59e0b;
                        padding: 12px 16px;
                        border-radius: 4px;
                        margin: 24px 0;
                    }
                    .expiry-notice strong {
                        color: #92400e;
                    }
                    .warning-box {
                        background: #fef2f2;
                        border-left: 4px solid #ef4444;
                        padding: 12px 16px;
                        border-radius: 4px;
                        margin: 24px 0;
                    }
                    .warning-box p {
                        color: #991b1b;
                        font-size: 14px;
                        margin: 0;
                    }
                    .info-section {
                        margin-top: 32px;
                        padding-top: 24px;
                        border-top: 1px solid #e5e7eb;
                    }
                    .info-text {
                        color: #6b7280;
                        font-size: 12px;
                        line-height: 1.8;
                    }
                    .info-text strong {
                        color: #374151;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 24px 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    .footer-text {
                        color: #6b7280;
                        font-size: 12px;
                    }
                    .footer-brand {
                        color: #111827;
                        font-weight: 600;
                    }
                    @media only screen and (max-width: 600px) {
                        body { padding: 20px 10px; }
                        .branding-section, .content-section, .footer {
                            padding: 24px 20px;
                        }
                        .title {
                            font-size: 24px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="branding-section">
                        <div class="brand-badge">SALOONY</div>
                        <div class="brand-subtitle">SALON SOLUTION</div>
                    </div>
                    <div class="content-section">
                        <div class="welcome-text">Password Reset Request</div>
                        <h1 class="title">Reset Your Password</h1>
                        <p class="instruction">We received a request to reset your password. Please click the button below to create a new password.</p>
                        
                        <p class="message">Hello <strong>%s</strong>,</p>
                        <p class="message">Click the button below to reset your password:</p>
                        
                        <div class="button-container">
                            <a href="%s" class="button">Reset Password</a>
                        </div>
                        
                        <p class="link-text">Or copy and paste this link into your browser:<br>
                            <a href="%s" class="link-url">%s</a>
                        </p>
                        
                        <div class="expiry-notice">
                            <strong>⏰ This link will expire in %d minutes.</strong>
                        </div>
                        
                        <div class="warning-box">
                            <p><strong>⚠️ Security Notice:</strong> If you didn't request this password reset, please ignore this email. Your password will remain unchanged.</p>
                        </div>
                        
                        <div class="info-section">
                            <p class="info-text">
                                <strong>Request Details:</strong><br>
                                IP Address: <strong>%s</strong><br>
                            If this wasn't you, please contact support immediately.
                        </p>
                        </div>
                    </div>
                    <div class="footer">
                        <p class="footer-text">
                            Best regards,<br>
                            <span class="footer-brand">Saloony Team</span>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """;
        
        return String.format(template, 
            userName != null ? userName : "User", 
            resetLink != null ? resetLink : "", 
            resetLink != null ? resetLink : "", 
            resetLink != null ? resetLink : "", 
            expirationMinutes, 
            ipAddress != null ? ipAddress : "Unknown");
    }

    /**
     * Build HTML email template for password reset confirmation
     */
    private String buildPasswordResetConfirmationEmailHtml(String userName) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #fce4ec 0%%, #ffffff 100%%);
                        padding: 40px 20px;
                        line-height: 1.6;
                    }
                    .email-container { 
                        max-width: 600px; 
                        margin: 0 auto;
                        background: #ffffff;
                        border-radius: 16px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .branding-section {
                        background: linear-gradient(135deg, #2d2d2d 0%%, #1a1a1a 100%%);
                        padding: 40px 30px;
                        color: #ffffff;
                    }
                    .brand-badge {
                        display: inline-block;
                        background: #ec4899;
                        color: white;
                        padding: 8px 16px;
                        border-radius: 8px;
                        font-weight: bold;
                        font-size: 18px;
                        letter-spacing: 1px;
                        margin-bottom: 12px;
                    }
                    .brand-subtitle {
                        color: #a0a0a0;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 24px;
                    }
                    .content-section {
                        padding: 40px 30px;
                        background: #ffffff;
                    }
                    .welcome-text {
                        color: #6b7280;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 8px;
                    }
                    .title {
                        color: #111827;
                        font-size: 28px;
                        font-weight: bold;
                        margin-bottom: 12px;
                    }
                    .instruction {
                        color: #6b7280;
                        font-size: 14px;
                        margin-bottom: 32px;
                    }
                    .message {
                        color: #374151;
                        font-size: 16px;
                        margin-bottom: 24px;
                    }
                    .message strong {
                        color: #111827;
                    }
                    .success-box {
                        background: #d1fae5;
                        border-left: 4px solid #10b981;
                        padding: 16px;
                        border-radius: 8px;
                        margin: 24px 0;
                        text-align: center;
                    }
                    .success-box p {
                        color: #065f46;
                        font-size: 16px;
                        font-weight: 600;
                        margin: 0;
                    }
                    .warning-box {
                        background: #fef2f2;
                        border-left: 4px solid #ef4444;
                        padding: 12px 16px;
                        border-radius: 4px;
                        margin: 24px 0;
                    }
                    .warning-box p {
                        color: #991b1b;
                        font-size: 14px;
                        margin: 0;
                    }
                    .button-container {
                        text-align: center;
                        margin: 32px 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 14px 32px;
                        background: #8b5cf6;
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 16px;
                        transition: background 0.3s ease;
                    }
                    .button:hover {
                        background: #7c3aed;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 24px 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    .footer-text {
                        color: #6b7280;
                        font-size: 12px;
                    }
                    .footer-brand {
                        color: #111827;
                        font-weight: 600;
                    }
                    @media only screen and (max-width: 600px) {
                        body { padding: 20px 10px; }
                        .branding-section, .content-section, .footer {
                            padding: 24px 20px;
                        }
                        .title {
                            font-size: 24px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="branding-section">
                        <div class="brand-badge">SALOONY</div>
                        <div class="brand-subtitle">SALON SOLUTION</div>
                    </div>
                    <div class="content-section">
                        <div class="welcome-text">Password Reset Confirmation</div>
                        <h1 class="title">Password Reset Successful</h1>
                        <p class="instruction">Your password has been successfully changed.</p>
                        
                        <p class="message">Hello <strong>%s</strong>,</p>
                        
                        <div class="success-box">
                            <p>✅ Your password has been successfully reset!</p>
                        </div>
                        
                        <p class="message">You can now log in to your account using your new password.</p>
                        
                        <div class="button-container">
                            <a href="http://localhost:3000/signin" class="button">Login to Your Account</a>
                    </div>
                        
                        <div class="warning-box">
                            <p><strong>⚠️ Security Notice:</strong> If you did not make this change, please contact support immediately.</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p class="footer-text">
                            Best regards,<br>
                            <span class="footer-brand">Saloony Team</span>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """;
        
        return String.format(template, userName != null ? userName : "User");
    }
}

