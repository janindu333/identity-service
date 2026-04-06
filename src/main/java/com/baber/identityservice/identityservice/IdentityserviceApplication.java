package com.baber.identityservice.identityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableDiscoveryClient
@SpringBootApplication
@EnableR2dbcRepositories
@EnableWebFlux

@PropertySource("classpath:application-${spring.profiles.active}.properties")
public class IdentityserviceApplication {
	public static void main(String[] args) {

		SpringApplication.run(IdentityserviceApplication.class, args);
	}

}
