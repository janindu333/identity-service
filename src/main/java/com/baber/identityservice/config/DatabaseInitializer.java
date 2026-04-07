package com.baber.identityservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0) // Run first, before RolesAndPermissionsInitializer
public class DatabaseInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing database tables...");
        
        try {
            // Create blacklisted_tokens table if it doesn't exist
            createBlacklistedTokensTable();
            
            logger.info("Database initialization completed successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to initialize database tables", e);
            throw e;
        }
    }
    
    private void createBlacklistedTokensTable() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS blacklisted_tokens (
                token_hash VARCHAR(64) PRIMARY KEY,
                token VARCHAR(1000) NOT NULL,
                username VARCHAR(100),
                reason VARCHAR(500),
                expiration_time DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                active BOOLEAN DEFAULT TRUE,
                
                INDEX idx_token_hash (token_hash),
                INDEX idx_username (username),
                INDEX idx_expiration_time (expiration_time),
                INDEX idx_active (active),
                INDEX idx_created_at (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try {
            jdbcTemplate.execute(createTableSQL);
            logger.info("blacklisted_tokens table created/verified successfully");
            
            // Check if table has any data
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM blacklisted_tokens WHERE active = TRUE", 
                Integer.class
            );
            
            logger.info("Current blacklisted tokens count: {}", count != null ? count : 0);
            
        } catch (Exception e) {
            logger.error("Failed to create blacklisted_tokens table", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
} 