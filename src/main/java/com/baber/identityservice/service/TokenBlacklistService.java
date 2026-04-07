package com.baber.identityservice.service;

import com.baber.identityservice.entity.BlacklistedToken;
import com.baber.identityservice.repository.BlacklistedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;
    
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    /**
     * Add a token to the blacklist (Redis + MySQL hybrid)
     */
    @Transactional
    public void blacklistToken(String token, String username, String reason, int expirationHours) {
        try {
            String tokenHash = BlacklistedToken.generateTokenHash(token);
            String redisKey = BLACKLIST_PREFIX + tokenHash;
            
            // Ensure minimum expiration time (1 hour)
            int actualExpiration = Math.max(expirationHours, 1);
            LocalDateTime expirationTime = LocalDateTime.now().plusHours(actualExpiration);
            
            // 1. Store in Redis (fast lookup)
            redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(actualExpiration));
            
            // 2. Store in MySQL (persistence)
            BlacklistedToken blacklistedToken = new BlacklistedToken();
            blacklistedToken.setTokenHash(tokenHash);
            blacklistedToken.setToken(token);
            blacklistedToken.setUsername(username);
            blacklistedToken.setReason(reason);
            blacklistedToken.setExpirationTime(expirationTime);
            blacklistedToken.setActive(true);
            blacklistedToken.setCreatedAt(LocalDateTime.now());
            
            blacklistedTokenRepository.save(blacklistedToken);
            
            logger.info("Token blacklisted successfully for user: {}, reason: {}, expiration: {} hours", 
                       username, reason, actualExpiration);
            
        } catch (Exception e) {
            logger.error("Failed to blacklist token for user: {}", username, e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }
    
    /**
     * Check if a token is blacklisted (Redis first, then MySQL fallback)
     */
    public boolean isBlacklisted(String token) {
        try {
            String tokenHash = BlacklistedToken.generateTokenHash(token);
            String redisKey = BLACKLIST_PREFIX + tokenHash;
            
            // 1. Check Redis first (fast lookup)
            Boolean isInRedis = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(isInRedis)) {
                logger.debug("Token found in Redis blacklist: {}", tokenHash);
                return true;
            }
            
            // 2. Check MySQL (persistent fallback)
            Optional<BlacklistedToken> blacklistedToken = blacklistedTokenRepository.findByTokenHashAndActiveTrue(tokenHash);
            if (blacklistedToken.isPresent()) {
                BlacklistedToken tokenEntity = blacklistedToken.get();
                
                // Check if token has expired
                if (tokenEntity.getExpirationTime().isBefore(LocalDateTime.now())) {
                    // Remove expired token
                    removeFromBlacklist(token);
                    return false;
                }
                
                // Add back to Redis if found in database but not in Redis
                redisTemplate.opsForValue().set(redisKey, "1", 
                    Duration.between(LocalDateTime.now(), tokenEntity.getExpirationTime()));
                
                logger.debug("Token found in MySQL blacklist, restored to Redis: {}", tokenHash);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking token blacklist status", e);
            // Fail secure - if we can't check, assume it's blacklisted
            return true;
        }
    }
    
    /**
     * Remove a token from blacklist (Redis + MySQL)
     */
    @Transactional
    public void removeFromBlacklist(String token) {
        try {
            String tokenHash = BlacklistedToken.generateTokenHash(token);
            String redisKey = BLACKLIST_PREFIX + tokenHash;
            
            // 1. Remove from Redis
            redisTemplate.delete(redisKey);
            
            // 2. Soft delete from MySQL
            blacklistedTokenRepository.softDeleteByTokenHash(tokenHash);
            
            logger.info("Token removed from blacklist: {}", tokenHash);
                    
        } catch (Exception e) {
            logger.error("Failed to remove token from blacklist: {}", token, e);
            throw new RuntimeException("Failed to remove token from blacklist", e);
        }
    }
    
    /**
     * Get blacklist statistics for monitoring
     */
    public BlacklistStats getBlacklistStats() {
        try {
            // Count in Redis
            Set<String> redisKeys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            long redisCount = redisKeys != null ? redisKeys.size() : 0;
            
            // Count in MySQL
            long databaseCount = blacklistedTokenRepository.countByActiveTrue();
            
            return new BlacklistStats(redisCount, databaseCount);
            
        } catch (Exception e) {
            logger.error("Failed to get blacklist statistics", e);
            return new BlacklistStats(0, 0);
        }
    }
    
    /**
     * Cleanup expired tokens (scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<BlacklistedToken> expiredTokens = blacklistedTokenRepository.findExpiredTokens(now);
            
            for (BlacklistedToken token : expiredTokens) {
                // Remove from Redis
                String redisKey = BLACKLIST_PREFIX + token.getTokenHash();
                redisTemplate.delete(redisKey);
                
                // Soft delete from MySQL
                token.setActive(false);
                blacklistedTokenRepository.save(token);
            }
            
            logger.info("Cleaned up {} expired tokens", expiredTokens.size());
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired tokens", e);
        }
    }
    
    /**
     * Rebuild Redis cache from MySQL database (for recovery after Redis restart)
     */
    public void rebuildCacheFromDatabase() {
        try {
            List<BlacklistedToken> activeTokens = blacklistedTokenRepository.findByActiveTrue();
            LocalDateTime now = LocalDateTime.now();
            
            int restoredCount = 0;
            for (BlacklistedToken token : activeTokens) {
                // Skip expired tokens
                if (token.getExpirationTime().isBefore(now)) {
                    continue;
                }
                
                // Add to Redis
                String redisKey = BLACKLIST_PREFIX + token.getTokenHash();
                Duration remainingTime = Duration.between(now, token.getExpirationTime());
                
                if (remainingTime.isPositive()) {
                    redisTemplate.opsForValue().set(redisKey, "1", remainingTime);
                    restoredCount++;
                }
            }
            
            logger.info("Rebuilt Redis cache with {} tokens from database", restoredCount);
            
        } catch (Exception e) {
            logger.error("Failed to rebuild cache from database", e);
        }
    }
    
    /**
     * Statistics class for monitoring
     */
    public static class BlacklistStats {
        private final long redisCount;
        private final long databaseCount;
        
        public BlacklistStats(long redisCount, long databaseCount) {
            this.redisCount = redisCount;
            this.databaseCount = databaseCount;
        }
        
        public long getRedisCount() { return redisCount; }
        public long getDatabaseCount() { return databaseCount; }
        public long getTotalCount() { return Math.max(redisCount, databaseCount); }
    }
} 