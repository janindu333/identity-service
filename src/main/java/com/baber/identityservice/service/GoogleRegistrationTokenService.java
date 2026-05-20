package com.baber.identityservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.baber.identityservice.config.JwtSigningKeySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class GoogleRegistrationTokenService {

    private static final String TOKEN_TYPE = "GOOGLE_REGISTRATION";
    private static final long TTL_MS = 15 * 60 * 1000L;

    @Value("${jwt.secret}")
    private String secret;

    public String create(String keycloakUserId, String email, String keycloakRefreshToken) {
        return Jwts.builder()
                .setSubject(email == null ? keycloakUserId : email)
                .claim("tokenType", TOKEN_TYPE)
                .claim("keycloakUserId", keycloakUserId)
                .claim("email", email)
                .claim("kcRefresh", keycloakRefreshToken)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TTL_MS))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!TOKEN_TYPE.equals(claims.get("tokenType", String.class))) {
                throw new GoogleAuthService.GoogleAuthException("Invalid registration token");
            }
            String keycloakUserId = claims.get("keycloakUserId", String.class);
            String email = claims.get("email", String.class);
            String kcRefresh = claims.get("kcRefresh", String.class);
            if (keycloakUserId == null || keycloakUserId.isBlank()) {
                throw new GoogleAuthService.GoogleAuthException("Invalid registration token");
            }
            return new ParsedToken(keycloakUserId, email, kcRefresh);
        } catch (JwtException e) {
            throw new GoogleAuthService.GoogleAuthException("Registration token is invalid or expired", e);
        }
    }

    private Key getSignKey() {
        return JwtSigningKeySupport.hmacKeyFromSecret(secret);
    }

    public record ParsedToken(String keycloakUserId, String email, String keycloakRefreshToken) {
    }
}
