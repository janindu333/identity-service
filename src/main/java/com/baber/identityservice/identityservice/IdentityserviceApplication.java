package com.baber.identityservice.identityservice;

import com.baber.identityservice.identityservice.config.ServiceLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class IdentityserviceApplication {

	// Create an instance of ServiceLogger
	private static final ServiceLogger logger = new ServiceLogger(IdentityserviceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(IdentityserviceApplication.class, args);

		// Log a message when the application starts
		logger.info("application started successfully.");
	}
}
