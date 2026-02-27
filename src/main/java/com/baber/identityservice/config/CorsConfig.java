package com.baber.identityservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    // CORS is handled by API Gateway, so we disable it here to avoid duplication
    // @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // CORS handled by API Gateway
                // registry.addMapping("/**")
                // .allowedOrigins("http://localhost:3000")
                // .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // .allowedHeaders("*")
                // .allowCredentials(true);
            }
        };
    }
}
