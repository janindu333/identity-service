package com.baber.identityservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate with client-side load balancing.
 *
 * This allows identity-service to call other microservices by their
 * Eureka service name (e.g., "saloon-service").
 */
@Configuration
public class RestTemplateConfig {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    /** Saloon summary can be slow when aggregating hours/services across salons. */
    private static final int READ_TIMEOUT_MS = 15000;

    private RestTemplate withTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(requestFactory);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return withTimeouts();
    }

    /**
     * Direct HTTP rest template for Kubernetes/CoreDNS service URLs.
     */
    @Bean(name = "internalServiceRestTemplate")
    public RestTemplate internalServiceRestTemplate() {
        return withTimeouts();
    }

    @Bean(name = "keycloakRestTemplate")
    public RestTemplate keycloakRestTemplate() {
        return withTimeouts();
    }
}

