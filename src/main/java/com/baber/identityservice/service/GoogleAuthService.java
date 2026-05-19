package com.baber.identityservice.service;

import com.baber.identityservice.config.GoogleAuthProperties;
import com.baber.identityservice.config.ServiceLogger;
import com.baber.identityservice.dto.GoogleAuthorizeUrlResponse;
import com.baber.identityservice.dto.GoogleCallbackResponse;
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

    public static final int REQUIRES_ROLE_SELECTION_ERROR_CODE = 4101;
    private static final long CUSTOMER_ROLE_ID = 7L;
    private static final long OWNER_ROLE_ID = 3L;

    private final ServiceLogger logger = new ServiceLogger(GoogleAuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GoogleAuthProperties properties;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final KeycloakService keycloakService;
    private final AuthService authService;
    private final RoleRepository roleRepository;
    private final GoogleRegistrationTokenService registrationTokenService;

    public GoogleAuthService(
            GoogleAuthProperties properties,
            GoogleTokenVerifier googleTokenVerifier,
            KeycloakService keycloakService,
            AuthService authService,
            RoleRepository roleRepository,
            GoogleRegistrationTokenService registrationTokenService) {
        this.properties = properties;
        this.googleTokenVerifier = googleTokenVerifier;
        this.keycloakService = keycloakService;
        this.authService = authService;
        this.roleRepository = roleRepository;
        this.registrationTokenService = registrationTokenService;
    }

    public void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new GoogleAuthDisabledException("Google sign-in is not enabled");
        }
    }

    public GoogleAuthorizeUrlResponse buildAuthorizeUrl(String redirectUri, Long roleId, String prompt) {
        ensureEnabled();
        String resolvedRedirect = resolveRedirectUri(redirectUri);
        validateRedirectUri(resolvedRedirect);
        String state = encodeState(roleId);
        String url = keycloakService.buildGoogleAuthorizationUrl(
                resolvedRedirect, state, sanitizeOidcPrompt(prompt));
        return new GoogleAuthorizeUrlResponse(url, state);
    }

    private static String sanitizeOidcPrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return null;
        }
        String trimmed = prompt.trim();
        if (!trimmed.matches("^[a-zA-Z0-9 _-]+$")) {
            throw new GoogleAuthException("Invalid prompt parameter");
        }
        return trimmed;
    }

    public GoogleCallbackOutcome handleCallback(
            String code,
            String redirectUri,
            String state,
            Long role,
            String registrationToken,
            boolean rememberMe) {
        ensureEnabled();
        if (StringUtils.hasText(registrationToken)) {
            validateRegistrationTokenFormat(registrationToken.trim());
            return completeWithRegistrationToken(registrationToken.trim(), role, rememberMe);
        }
        if (!StringUtils.hasText(code) || !StringUtils.hasText(redirectUri)) {
            throw new GoogleAuthException("code and redirectUri are required");
        }
        validateRedirectUri(redirectUri.trim());
        try {
            KeycloakService.TokenGrantResponse grant =
                    keycloakService.exchangeAuthorizationCode(code.trim(), redirectUri.trim());
            return completeAfterCodeExchange(grant, state, role, rememberMe);
        } catch (HttpClientErrorException e) {
            logger.warn("Google authorization code exchange failed: HTTP " + e.getStatusCode().value());
            if (e.getStatusCode().value() == 400) {
                throw new GoogleAuthException(
                        "Authorization code is invalid or already used. If you already received errorCode 4101, "
                                + "call this endpoint again with registrationToken and role only (do not resend code).",
                        e);
            }
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

    private GoogleCallbackOutcome completeWithRegistrationToken(
            String registrationToken, Long role, boolean rememberMe) {
        if (role == null) {
            throw new GoogleAuthException("role is required (3 = owner, 7 = customer)");
        }
        validateSignupRoleId(role);
        GoogleRegistrationTokenService.ParsedToken parsed = registrationTokenService.parse(registrationToken);
        if (isExistingAppAccount(parsed.keycloakUserId())) {
            KeycloakService.TokenGrantResponse grant = refreshGrant(parsed);
            return GoogleCallbackOutcome.login(finalizeLogin(grant, rememberMe), true);
        }
        boolean roleAssigned = applySignupRoleIfNeeded(parsed.keycloakUserId(), role);
        authService.ensureLocalUserProfilePublic(parsed.keycloakUserId());
        KeycloakService.TokenGrantResponse grant = refreshGrant(parsed);
        if (roleAssigned && StringUtils.hasText(grant.refreshToken())) {
            grant = keycloakService.refresh(grant.refreshToken());
        }
        return GoogleCallbackOutcome.login(finalizeLogin(grant, rememberMe), false);
    }

    private GoogleCallbackOutcome completeAfterCodeExchange(
            KeycloakService.TokenGrantResponse grant,
            String state,
            Long requestRole,
            boolean rememberMe) {
        String keycloakUserId = authService.extractSubjectFromAccessToken(grant.accessToken());
        if (!StringUtils.hasText(keycloakUserId)) {
            throw new GoogleAuthException("Could not resolve user from Keycloak access token");
        }
        String email = resolveEmailFromAccessToken(grant.accessToken(), keycloakUserId);
        Long effectiveRole = resolveEffectiveRole(requestRole, state);

        if (isExistingAppAccount(keycloakUserId)) {
            logger.info("Google callback: existing app account for keycloakUserId=" + keycloakUserId);
            return GoogleCallbackOutcome.login(
                    finalizeLoginAfterRoleSync(grant, keycloakUserId, effectiveRole, rememberMe),
                    true);
        }

        if (effectiveRole == null) {
            logger.info("Google callback: role selection required for keycloakUserId=" + keycloakUserId);
            String token = registrationTokenService.create(
                    keycloakUserId,
                    email,
                    grant.refreshToken());
            return GoogleCallbackOutcome.roleSelectionRequired(
                    GoogleCallbackResponse.roleSelectionRequired(token, email));
        }

        validateSignupRoleId(effectiveRole);
        return GoogleCallbackOutcome.login(
                finalizeLoginAfterRoleSync(grant, keycloakUserId, effectiveRole, rememberMe),
                false);
    }

    private AuthService.KeycloakLoginResult finalizeLoginAfterRoleSync(
            KeycloakService.TokenGrantResponse grant,
            String keycloakUserId,
            Long roleId,
            boolean rememberMe) {
        boolean roleAssigned = applySignupRoleIfNeeded(keycloakUserId, roleId);
        authService.ensureLocalUserProfilePublic(keycloakUserId);
        if (roleAssigned && StringUtils.hasText(grant.refreshToken())) {
            grant = keycloakService.refresh(grant.refreshToken());
        }
        return finalizeLogin(grant, rememberMe);
    }

    private KeycloakService.TokenGrantResponse refreshGrant(GoogleRegistrationTokenService.ParsedToken parsed) {
        if (!StringUtils.hasText(parsed.keycloakRefreshToken())) {
            throw new GoogleAuthException("Registration session expired; sign in with Google again");
        }
        return keycloakService.refresh(parsed.keycloakRefreshToken());
    }

    private Long resolveEffectiveRole(Long requestRole, String state) {
        if (requestRole != null) {
            return requestRole;
        }
        return decodeRoleIdFromState(state);
    }

    private void validateSignupRoleId(Long roleId) {
        if (roleId == null || (roleId != OWNER_ROLE_ID && roleId != CUSTOMER_ROLE_ID)) {
            throw new GoogleAuthException("role must be 3 (owner) or 7 (customer)");
        }
    }

    private String resolveEmailFromAccessToken(String accessToken, String keycloakUserId) {
        String email = authService.extractUsernameFromAccessToken(accessToken);
        if (email != null && email.contains("@")) {
            return email;
        }
        KeycloakService.KeycloakUserProfile profile = keycloakService.findUserById(keycloakUserId);
        if (profile != null && StringUtils.hasText(profile.email())) {
            return profile.email();
        }
        return email;
    }

    private AuthService.KeycloakLoginResult finalizeLogin(KeycloakService.TokenGrantResponse grant, boolean rememberMe) {
        String loginHint = authService.extractUsernameFromAccessToken(grant.accessToken());
        if (!StringUtils.hasText(loginHint)) {
            throw new GoogleAuthException("Could not resolve user from Keycloak access token");
        }
        String keycloakUserId = authService.extractSubjectFromAccessToken(grant.accessToken());
        if (StringUtils.hasText(keycloakUserId)) {
            authService.ensureLocalUserProfilePublic(keycloakUserId);
        }
        TokenResponse tokenResponse = authService.createLoginResponse(grant.accessToken(), loginHint);
        tokenResponse.setExpiresIn(grant.expiresIn());
        return new AuthService.KeycloakLoginResult(tokenResponse, grant.refreshToken());
    }

    private void provisionKeycloakUserIfNeeded(GoogleTokenVerifier.GoogleUserClaims googleUser, Long roleId) {
        KeycloakService.KeycloakUserProfile existing = keycloakService.findUserByEmail(googleUser.email());
        if (existing != null) {
            applySignupRoleIfNeeded(existing.id(), roleId);
            authService.ensureLocalUserProfilePublic(existing.id());
            return;
        }

        Role role = resolveRole(roleId);
        String realmRole = role.getName() == null ? "customer" : role.getName().trim().toLowerCase();

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
            validateSignupRoleId(roleId);
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

    private boolean isExistingAppAccount(String keycloakUserId) {
        if (!StringUtils.hasText(keycloakUserId)) {
            return false;
        }
        if (authService.findUserByKeycloakUserId(keycloakUserId) == null) {
            return false;
        }
        return hasBusinessRealmRole(keycloakUserId);
    }

    private boolean hasBusinessRealmRole(String keycloakUserId) {
        KeycloakService.KeycloakUserProfile profile = keycloakService.findUserById(keycloakUserId);
        if (profile == null || profile.realmRoles() == null) {
            return false;
        }
        return profile.realmRoles().stream()
                .anyMatch(r -> "owner".equalsIgnoreCase(r) || "customer".equalsIgnoreCase(r));
    }

    private void validateRegistrationTokenFormat(String registrationToken) {
        if (registrationToken.chars().filter(ch -> ch == '.').count() != 2) {
            throw new GoogleAuthException(
                    "Invalid registrationToken. Use the registrationToken from the 4101 response body, "
                            + "not the OAuth state query parameter from the redirect URL.");
        }
    }

    private Long decodeRoleIdFromState(String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(state.trim());
            JsonNode node = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            if (node.has("roleId") && !node.get("roleId").isNull()) {
                return node.get("roleId").asLong();
            }
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // Keycloak or third-party state — no embedded roleId
        }
        return null;
    }

    /**
     * Applies realm role from signup intent (authorize ?role= or ID token body).
     * @return true if a new realm role was assigned (caller may refresh tokens)
     */
    private boolean applySignupRoleIfNeeded(String keycloakUserId, Long roleId) {
        if (roleId == null) {
            return false;
        }
        Role role = resolveRole(roleId);
        String realmRole = role.getName() == null ? "customer" : role.getName().trim().toLowerCase();
        KeycloakService.KeycloakUserProfile profile = keycloakService.findUserById(keycloakUserId);
        if (profile != null && profile.realmRoles() != null
                && profile.realmRoles().stream().anyMatch(r -> realmRole.equalsIgnoreCase(r))) {
            return false;
        }
        keycloakService.assignRealmRoleToUser(keycloakUserId, realmRole);
        logger.info("Assigned Keycloak realm role '" + realmRole + "' for Google sign-in user " + keycloakUserId);
        return true;
    }

    public static final class GoogleCallbackOutcome {
        private final AuthService.KeycloakLoginResult loginResult;
        private final GoogleCallbackResponse roleSelectionResponse;
        private final boolean existingAccount;
        private final boolean roleSelection;

        private GoogleCallbackOutcome(
                AuthService.KeycloakLoginResult loginResult,
                GoogleCallbackResponse roleSelectionResponse,
                boolean existingAccount,
                boolean roleSelection) {
            this.loginResult = loginResult;
            this.roleSelectionResponse = roleSelectionResponse;
            this.existingAccount = existingAccount;
            this.roleSelection = roleSelection;
        }

        public static GoogleCallbackOutcome roleSelectionRequired(GoogleCallbackResponse response) {
            return new GoogleCallbackOutcome(null, response, false, true);
        }

        public static GoogleCallbackOutcome login(AuthService.KeycloakLoginResult result, boolean existingAccount) {
            return new GoogleCallbackOutcome(result, null, existingAccount, false);
        }

        public boolean isRoleSelection() {
            return roleSelection;
        }

        public GoogleCallbackResponse roleSelectionResponse() {
            return roleSelectionResponse;
        }

        public AuthService.KeycloakLoginResult loginResult() {
            return loginResult;
        }

        public boolean existingAccount() {
            return existingAccount;
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
