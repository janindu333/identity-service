package com.baber.identityservice.config;

import com.baber.identityservice.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job to cleanup expired password reset tokens
 * Runs daily at 2 AM to remove expired tokens from database
 */
@Component
public class PasswordResetTokenCleanupJob {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenCleanupJob.class);

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Cleanup expired tokens daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired password reset tokens");
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = passwordResetTokenRepository.deleteExpiredTokens(now);
            logger.info("Cleaned up {} expired password reset tokens", deletedCount);
        } catch (Exception e) {
            logger.error("Error during password reset token cleanup: " + e.getMessage(), e);
        }
    }
}

