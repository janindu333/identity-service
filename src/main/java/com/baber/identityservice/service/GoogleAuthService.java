package com.baber.identityservice.service;

import com.baber.identityservice.config.GoogleAuthProperties;
import com.baber.identityservice.config.ServiceLogger;
import com.baber.identityservice.dto.GoogleAuthorizeUrlResponse;
import com.baber.identityservice.dto.TokenResponse;
import com.baber.identityservice.entity.Role;
import com.baber.identityservice.repository.RoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private final ServiceLogger logger = new ServiceLogger(GoogleAuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GoogleAuthProperties properties;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final KeycloakService keycloakService;
    private final AuthService authService;
    private final RoleRepository roleRepository;

    public GoogleAuthService(
            GoogleAuthProperties properties,
            GoogleTokenVerifier googleTokenVerifier,
            KeycloakService keycloakService,
            AuthService authService,
            RoleRepository roleRepository) {
        this.properties = properties;
        this.googleTokenVerifier = googleTokenVerifier;
        this.keycloakService = keycloakService;
        this.authService = authService;
        this.roleRepository = roleRepository;
    }

    public void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new GoogleAuthDisabledException("Google sign-in is not enabled");
        }
    }

    public GoogleAuthorizeUrlResponse buildAuthorizeUrl(String redirectUri, Long roleId) {
        ensureEnabled();
        String resolvedRedirect = resolveRedirectUri(redirectUri);
        validateRedirectUri(resolvedRedirect);
        String state = encodeState(roleId);
        String url = keycloakService.buildGoogleAuthorizationUrl(resolvedRedirect, state);
        return new GoogleAuthorizeUrlResponse(url, state);
    }

    public AuthService.KeycloakLoginResult completeAuthorizationCode(String code, String redirectUri, boolean rememberMe) {
        ensureEnabled();
        validateRedirectUri(redirectUri);
        try {
            KeycloakService.TokenGrantResponse grant =
                    keycloakService.exchangeAuthorizationCode(code.trim(), redirectUri.trim());
            return finalizeLogin(grant, rememberMe);
        } catch (HttpClientErrorException e) {
            logger.warn("Google authorization code exchange failed: HTTP " + e.getStatusCode().value());
            throw new GoogleAuthException("Google sign-in failed at identity provider", e);
        }
    }

    public AuthService.KeycloakLoginResult signInWithIdToken(String idToken, Long roleId, boolean rememberMe) {
        ensureEnabled();
        GoogleTokenVerifier.GoogleUserClaims googleUser = googleTokenVerifier.verify(idToken);
        provisionKeycloakUserIfNeeded(googleUser, roleId);

        try {
            KeycloakService.TokenGrantResponse grant = keycloakService.exchangeGoogleIdToken(idToken.trim());
            return finalizeLogin(grant, rememberMe);
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : "";
            logger.warn("Google ID token exchange failed: HTTP " + e.getStatusCode().value() + " body=" + body);
            throw new GoogleAuthException(
                    "Google token exchange failed. Ensure Google is configured as an Identity Provider in Keycloak (alias: google) "
                            + "and token exchange is permitted for this client.",
                    e);
        }
    }

    private AuthService.KeycloakLoginResult finalizeLogin(KeycloakService.TokenGrantResponse grant, boolean rememberMe) {
        String username = authService.extractUsernameFromAccessToken(grant.accessToken());
        if (!StringUtils.hasText(username)) {
            throw new GoogleAuthException("Could not resolve user from Keycloak access token");
        }
        TokenResponse tokenResponse = authService.createLoginResponse(grant.accessToken(), username);
        tokenResponse.setExpiresIn(grant.expiresIn());
        return new AuthService.KeycloakLoginResult(tokenResponse, grant.refreshToken());
    }

    private void provisionKeycloakUserIfNeeded(GoogleTokenVerifier.GoogleUserClaims googleUser, Long roleId) {
        KeycloakService.KeycloakUserProfile existing = keycloakService.findUserByEmail(googleUser.email());
        if (existing != null) {
            authService.ensureLocalUserProfilePublic(existing.id());
            return;
        }

        Role role = resolveRole(roleId);
        String realmRole = role.getName() == null ? "customer" : role.getName().trim().toLowerCase();
        if ("owner".equals(realmRole)) {
            throw new GoogleAuthException("New owner accounts cannot be created via Google sign-in. Use email sign-up.");
        }

        String keycloakUserId = keycloakService.createSocialUser(
                googleUser.email(),
                googleUser.firstName(),
                googleUser.lastName(),
                realmRole
        );
        try {
            keycloakService.linkGoogleFederatedIdentity(
                    keycloakUserId,
                    googleUser.googleSubject(),
                    googleUser.email()
            );
        } catch (Exception linkError) {
            logger.warn("Could not link Google federated identity for " + googleUser.email()
                    + ": " + linkError.getMessage());
        }
        authService.ensureLocalUserProfilePublic(keycloakUserId);
        logger.info("Provisioned Keycloak user for Google sign-in: " + googleUser.email());
    }

    private Role resolveRole(Long roleId) {
        if (roleId != null) {
            Optional<Role> requested = roleRepository.findById(roleId.intValue());
            if (requested.isPresent()) {
                return requested.get();
            }
            throw new GoogleAuthException("Invalid role ID: " + roleId);
        }
        return roleRepository.findByName("customer")
                .orElseThrow(() -> new GoogleAuthException("Default customer role not found"));
    }

    private String resolveRedirectUri(String redirectUri) {
        if (StringUtils.hasText(redirectUri)) {
            return redirectUri.trim();
        }
        if (StringUtils.hasText(properties.getDefaultRedirectUri())) {
            return properties.getDefaultRedirectUri().trim();
        }
        throw new GoogleAuthException("redirectUri is required");
    }

    private void validateRedirectUri(String redirectUri) {
        String allowed = properties.getAllowedRedirectUris();
        if (!StringUtils.hasText(allowed)) {
            return;
        }
        boolean match = Arrays.stream(allowed.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(uri -> uri.equals(redirectUri));
        if (!match) {
            throw new GoogleAuthException("redirectUri is not allowed");
        }
    }

    private String encodeState(Long roleId) {
        try {
            String stateId = UUID.randomUUID().toString();
            JsonNode node = roleId != null
                    ? objectMapper.createObjectNode().put("s", stateId).put("roleId", roleId)
                    : objectMapper.createObjectNode().put("s", stateId);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(node));
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public static class GoogleAuthException extends RuntimeException {
        public GoogleAuthException(String message) {
            super(message);
        }

        public GoogleAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GoogleAuthDisabledException extends RuntimeException {
        public GoogleAuthDisabledException(String message) {
            super(message);
        }
    }
}
