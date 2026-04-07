package com.baber.identityservice.config;

import com.baber.identityservice.service.TokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2) // Run after DatabaseInitializer
public class StartupConfig implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Identity Service...");
        
        try {
            // Rebuild Redis cache from MySQL database
            logger.info("Rebuilding Redis cache from MySQL database...");
            tokenBlacklistService.rebuildCacheFromDatabase();
            
            logger.info("Identity Service started successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to rebuild Redis cache from database", e);
            // Don't throw exception - allow service to start even if Redis is down
            logger.warn("Service will continue without Redis cache. Blacklist will use database fallback.");
        }
    }
} 