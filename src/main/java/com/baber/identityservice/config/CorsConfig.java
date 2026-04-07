package com.baber.identityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    // Swagger UI is served from the API gateway, but the preflight request is still
    // validated by the target service. If this mapping is missing, identity-service
    // rejects preflight with "Invalid CORS request".
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080,http://127.0.0.1:8080}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow-all test: lets us confirm whether identity-service is the
        // component actually enforcing CORS for preflight.
        config.setAllowCredentials(false);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        config.addAllowedOriginPattern("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
