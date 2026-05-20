package com.baber.identityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Keycloak often issues tokens with {@code iss} = {@code http://localhost:9090/realms/...} while
 * pods fetch JWKS from {@code host.docker.internal}. Spring's issuer-only config rejects those tokens.
 * This decoder validates signatures via JWKS and accepts any issuer listed in {@code jwt.accepted-issuers}.
 */
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
public class KeycloakResourceServerJwtConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${jwt.accepted-issuers:}") String acceptedIssuersCsv
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> issuers = jwt -> validateIssuers(jwt, acceptedIssuersCsv);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, issuers));
        return decoder;
    }

    private static OAuth2TokenValidatorResult validateIssuers(Jwt jwt, String acceptedIssuersCsv) {
        if (!StringUtils.hasText(acceptedIssuersCsv)) {
            return OAuth2TokenValidatorResult.success();
        }
        String issRaw = jwt.getClaimAsString("iss");
        final String iss = issRaw != null ? issRaw : "";
        boolean ok = Arrays.stream(acceptedIssuersCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(allowed -> allowed.equals(iss));
        if (ok) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Unexpected iss: " + iss, null));
    }
}
