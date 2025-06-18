package com.baber.identityservice.identityservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceLogger {
    private final Logger logger;
    private final String serviceName = "identity-service";

    public ServiceLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public void info(String message) {
        logger.info("[{}] [loglevel-INFO] {}", serviceName, message);  // Adding loglevel-INFO explicitly
    }

    public void debug(String message) {
        logger.debug("[{}] [loglevel-DEBUG] {}", serviceName, message);  // Adding loglevel-DEBUG explicitly
    }

    public void error(String message) {
        logger.error("[{}] [loglevel-ERROR] {}", serviceName, message);  // Adding loglevel-ERROR explicitly
    }

    public void warn(String message) {
        logger.warn("[{}] [loglevel-WARN] {}", serviceName, message);  // Adding loglevel-WARN explicitly
    }

    public void trace(String message) {
        logger.trace("[{}] [loglevel-TRACE] {}", serviceName, message);  // Adding loglevel-TRACE explicitly
    }
    public void success(String message) {
        logger.info("[{}] [loglevel-SUCCESS] {}", serviceName, message);  // Adding loglevel-SUCCESS explicitly
    }
    public void apiRequest(String message) {
        logger.info("[{}] [loglevel-API-REQUEST] {}", serviceName, message);  // Adding loglevel-SUCCESS explicitly
    }
}
