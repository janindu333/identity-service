package com.baber.identityservice;

import com.baber.identityservice.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableConfigurationProperties(RateLimitProperties.class)
@EnableScheduling
public class IdentityserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityserviceApplication.class, args);
	}
}
