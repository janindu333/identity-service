package com.baber.identityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableDiscoveryClient
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class IdentityserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityserviceApplication.class, args);
	}
}
