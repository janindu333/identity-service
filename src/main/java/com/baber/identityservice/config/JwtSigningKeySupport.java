package com.baber.identityservice.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

/**
 * Resolves HMAC signing keys from configured secrets (standard Base64, Base64url, or plain text).
 */
public final class JwtSigningKeySupport {

    private JwtSigningKeySupport() {
    }

    public static Key hmacKeyFromSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret is not configured");
        }
        String trimmed = secret.trim();
        byte[] keyBytes = tryDecodeBase64(trimmed);
        if (keyBytes == null) {
            keyBytes = trimmed.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] tryDecodeBase64(String value) {
        try {
            return Decoders.BASE64.decode(value);
        } catch (RuntimeException ignored) {
            // not standard Base64 (e.g. contains '_' from base64url or plain text)
        }
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
