-- Create blacklisted_tokens table for Identity Service
-- This table stores blacklisted JWT tokens for persistence

USE saloon-service;

-- Drop table if exists (for development)
DROP TABLE IF EXISTS blacklisted_tokens;

-- Create blacklisted_tokens table
CREATE TABLE blacklisted_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    token VARCHAR(1000) NOT NULL,
    username VARCHAR(100),
    reason VARCHAR(500),
    expiration_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    
    -- Indexes for performance
    INDEX idx_token_hash (token_hash),
    INDEX idx_username (username),
    INDEX idx_expiration_time (expiration_time),
    INDEX idx_active (active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample data (optional - for testing)
-- INSERT INTO blacklisted_tokens (token_hash, token, username, reason, expiration_time, active) 
-- VALUES (
--     'sample_hash_123',
--     'sample.jwt.token',
--     'testuser',
--     'Testing blacklist functionality',
--     DATE_ADD(NOW(), INTERVAL 2 HOUR),
--     TRUE
-- );

-- Show table structure
DESCRIBE blacklisted_tokens;

-- Show sample data (if any)
SELECT COUNT(*) as total_blacklisted_tokens FROM blacklisted_tokens WHERE active = TRUE; 