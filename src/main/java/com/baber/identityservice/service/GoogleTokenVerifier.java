package com.baber.identityservice.service;

import com.baber.identityservice.config.GoogleAuthProperties;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final GoogleAuthProperties properties;
    private volatile JwtDecoder jwtDecoder;

    public GoogleTokenVerifier(GoogleAuthProperties properties) {
        this.properties = properties;
    }

    public GoogleUserClaims verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new IllegalArgumentException("Google ID token is required");
        }
        if (!StringUtils.hasText(properties.getClientId())) {
            throw new IllegalStateException("auth.google.client-id is not configured");
        }

        Jwt jwt;
        try {
            jwt = getDecoder().decode(idToken.trim());
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid Google ID token: " + e.getMessage(), e);
        }

        List<String> audiences = jwt.getAudience();
        if (audiences == null || !audiences.contains(properties.getClientId())) {
            throw new IllegalArgumentException("Google ID token audience does not match configured client id");
        }

        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaim("email_verified") instanceof Boolean b ? b : Boolean.FALSE;
        if (!StringUtils.hasText(email) || !emailVerified) {
            throw new IllegalArgumentException("Google account email is missing or not verified");
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");
        if (!StringUtils.hasText(givenName) && StringUtils.hasText(name)) {
            String[] parts = name.trim().split("\\s+", 2);
            givenName = parts[0];
            if (parts.length > 1 && !StringUtils.hasText(familyName)) {
                familyName = parts[1];
            }
        }

        return new GoogleUserClaims(
                email.trim().toLowerCase(),
                jwt.getSubject(),
                givenName == null ? "" : givenName,
                familyName == null ? "" : familyName
        );
    }

    private JwtDecoder getDecoder() {
        JwtDecoder local = jwtDecoder;
        if (local == null) {
            synchronized (this) {
                local = jwtDecoder;
                if (local == null) {
                    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
                    OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator();
                    OAuth2TokenValidator<Jwt> issuer = jwt -> {
                        String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
                        if (iss != null && GOOGLE_ISSUERS.contains(iss)) {
                            return OAuth2TokenValidatorResult.success();
                        }
                        return OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Unexpected Google iss: " + iss, null));
                    };
                    decoder.setJwtValidator(token -> {
                        OAuth2TokenValidatorResult ts = timestamp.validate(token);
                        if (ts.hasErrors()) {
                            return ts;
                        }
                        return issuer.validate(token);
                    });
                    jwtDecoder = decoder;
                    local = decoder;
                }
            }
        }
        return local;
    }

    public record GoogleUserClaims(String email, String googleSubject, String firstName, String lastName) {
    }
}
