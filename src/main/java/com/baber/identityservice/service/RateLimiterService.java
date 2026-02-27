package com.baber.identityservice.service;

import com.baber.identityservice.config.RateLimitProperties.Limit;
import com.baber.identityservice.config.ServiceLogger;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final ServiceLogger logger = new ServiceLogger(RateLimiterService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> script;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(Long.class);
        this.script.setScriptText(buildScript());
    }

    public RateLimitResult checkLimit(String key, Limit config) {
        if (config == null || config.getLimit() <= 0) {
            return RateLimitResult.allowed();
        }

        Duration window = Duration.ofSeconds(Math.max(1, config.getWindowSeconds()));
        List<String> keys = Collections.singletonList(key);

        try {
            Long ttlMillis = redisTemplate.execute(
                script,
                keys,
                String.valueOf(window.toMillis()),
                String.valueOf(config.getLimit())
            );

            if (ttlMillis == null || ttlMillis <= 0) {
                return RateLimitResult.allowed();
            }

            long retryAfterSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(ttlMillis));
            return RateLimitResult.blocked(retryAfterSeconds);
        } catch (DataAccessException ex) {
            logger.warn("Rate limiter fallback for key " + key + " due to redis issue: " + ex.getMessage());
            return RateLimitResult.allowed();
        }
    }

    private String buildScript() {
        // language=Lua
        return """
            local current = redis.call('incr', KEYS[1])
            if current == 1 then
                redis.call('pexpire', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return redis.call('pttl', KEYS[1])
            end
            return 0
            """;
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final long retryAfterSeconds;

        private RateLimitResult(boolean allowed, long retryAfterSeconds) {
            this.allowed = allowed;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static RateLimitResult allowed() {
            return new RateLimitResult(true, 0);
        }

        public static RateLimitResult blocked(long retryAfterSeconds) {
            return new RateLimitResult(false, retryAfterSeconds);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}

