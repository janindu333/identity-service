package com.baber.identityservice.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class KeycloakService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakService.class);

    private final RestTemplate restTemplate;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-client-id:${keycloak.client-id}}")
    private String adminClientId;

    @Value("${keycloak.admin-client-secret:${keycloak.client-secret}}")
    private String adminClientSecret;

    /**
     * When true, login with {@code rememberMe} requests Keycloak {@code scope=openid offline_access}
     * so refresh tokens follow offline-session rules (long-lived until revoked).
     * The Keycloak client must allow the {@code offline_access} client scope.
     */
    @Value("${keycloak.remember-me-offline-access:true}")
    private boolean rememberMeOfflineAccess;

    public KeycloakService(@Qualifier("keycloakRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TokenGrantResponse login(String username, String password) {
        return login(username, password, false);
    }

    /**
     * @param rememberMe if true and {@link #rememberMeOfflineAccess} is enabled, requests {@code offline_access} scope
     */
    public TokenGrantResponse login(String username, String password, boolean rememberMe) {
        MultiValueMap<String, String> form = buildResourceOwnerPasswordForm(username, password, rememberMe);
        try {
            return requestToken(form);
        } catch (HttpClientErrorException e) {
            if (rememberMe && rememberMeOfflineAccess && e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String body = e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : "";
                String lower = body.toLowerCase();
                if (lower.contains("scope") || lower.contains("offline")) {
                    log.warn("Keycloak rejected offline_access for this client; retrying login without offline scope. Hint: add client scope 'offline_access' in Keycloak admin.");
                    return login(username, password, false);
                }
            }
            throw e;
        }
    }

    private MultiValueMap<String, String> buildResourceOwnerPasswordForm(String username, String password, boolean rememberMe) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);
        if (rememberMe && rememberMeOfflineAccess) {
            form.add("scope", "openid offline_access");
        }
        return form;
    }

    public TokenGrantResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        return requestToken(form);
    }

    /**
     * List realm role names from Keycloak.
     * Used to mirror Keycloak roles into the local DB so DB roles remain a permission-mapping layer,
     * not an independent source of truth for role catalogs.
     */
    public List<String> listRealmRoleNames() {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                realmAdminRolesUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Keycloak role listing returned null body");
        }

        return body.stream()
                .map(m -> m.get("name"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public String createUser(String email,
                           String firstName,
                           String lastName,
                           String phone,
                           String password,
                           boolean emailVerified,
                           String realmRoleName) {
        String adminToken = getAdminToken();
        String usersUrl = realmAdminUsersUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "attributes", Map.of(
                        "phone", List.of(phone == null ? "" : phone)
                ),
                "enabled", true,
                "emailVerified", emailVerified,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
        );

        ResponseEntity<Void> response = restTemplate.exchange(
                usersUrl,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Void.class
        );

        HttpStatusCode status = response.getStatusCode();
        if (!status.is2xxSuccessful()) {
            throw new IllegalStateException("Keycloak user create failed: HTTP " + status.value());
        }

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                    "Keycloak user create returned success but no Location header; refusing to continue without user id");
        }

        String userId = location.substring(location.lastIndexOf('/') + 1);
        assignRealmRole(adminToken, userId, realmRoleName);
        return userId;
    }

    /**
     * Whether the realm already has a user with this email (or username equal to email, since signup sets both).
     */
    public boolean userExistsWithEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        if (countUsersForQuery(headers, "email", email, true) > 0) {
            return true;
        }
        return countUsersForQuery(headers, "username", email, true) > 0;
    }

    public KeycloakUserProfile findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        String url = authServerUrl + "/admin/realms/" + realm + "/users?email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&exact=true&max=1";

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> users = response.getBody();
        if (users == null || users.isEmpty()) {
            return null;
        }
        return toUserProfile(adminToken, users.get(0));
    }

    /**
     * Exact username search (signup sets {@code username} = email, so duplicates can exist on username alone).
     */
    public KeycloakUserProfile findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        String url = authServerUrl + "/admin/realms/" + realm + "/users?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&exact=true&max=1";

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> users = response.getBody();
        if (users == null || users.isEmpty()) {
            return null;
        }
        return toUserProfile(adminToken, users.get(0));
    }

    public KeycloakUserProfile findUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                realmAdminUsersUrl() + "/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> user = response.getBody();
        if (user == null || user.isEmpty()) {
            return null;
        }
        return toUserProfile(adminToken, user);
    }

    public void setEmailVerified(String userId, boolean emailVerified) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> getResp = restTemplate.exchange(
                realmAdminUsersUrl() + "/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> user = getResp.getBody();
        if (user == null) {
            return;
        }
        user.put("emailVerified", emailVerified);

        restTemplate.exchange(
                realmAdminUsersUrl() + "/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(user, headers),
                Void.class
        );
    }

    public void resetUserPassword(String userId, String newPassword) {
        if (userId == null || userId.isBlank() || newPassword == null || newPassword.isBlank()) {
            return;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> credential = Map.of(
                "type", "password",
                "temporary", false,
                "value", newPassword
        );

        restTemplate.exchange(
                realmAdminUsersUrl() + "/" + userId + "/reset-password",
                HttpMethod.PUT,
                new HttpEntity<>(credential, headers),
                Void.class
        );
    }

    public void sendVerifyEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Prefer execute-actions-email for broader Keycloak compatibility.
        // Fallback to send-verify-email for older/newer API differences.
        try {
            restTemplate.exchange(
                    realmAdminUsersUrl() + "/" + userId + "/execute-actions-email",
                    HttpMethod.PUT,
                    new HttpEntity<>(List.of("VERIFY_EMAIL"), headers),
                    Void.class
            );
        } catch (Exception primaryError) {
            log.warn("Keycloak execute-actions-email failed for user {}. Falling back to send-verify-email. Cause: {}",
                    userId, primaryError.getMessage());
            restTemplate.exchange(
                    realmAdminUsersUrl() + "/" + userId + "/send-verify-email",
                    HttpMethod.PUT,
                    new HttpEntity<>(headers),
                    Void.class
            );
        }
    }

    private int countUsersForQuery(HttpHeaders headers, String queryParam, String value, boolean exact) {
        String url = authServerUrl + "/admin/realms/" + realm + "/users?"
                + queryParam + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)
                + "&exact=" + exact + "&max=10";
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        List<Map<String, Object>> body = response.getBody();
        return body == null ? 0 : body.size();
    }

    @SuppressWarnings("unchecked")
    private KeycloakUserProfile toUserProfile(String adminToken, Map<String, Object> user) {
        String userId = stringVal(user.get("id"));
        String email = stringVal(user.get("email"));
        String username = stringVal(user.get("username"));
        String firstName = stringVal(user.get("firstName"));
        String lastName = stringVal(user.get("lastName"));
        Boolean emailVerified = user.get("emailVerified") instanceof Boolean b ? b : Boolean.FALSE;
        List<String> realmRoles = fetchRealmRoles(adminToken, userId);
        return new KeycloakUserProfile(userId, email, username, firstName, lastName, emailVerified, realmRoles);
    }

    private List<String> fetchRealmRoles(String adminToken, String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                realmAdminUsersUrl() + "/" + userId + "/role-mappings/realm",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> roles = response.getBody();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Map<String, Object> role : roles) {
            String roleName = stringVal(role.get("name"));
            if (roleName != null && !roleName.isBlank()) {
                result.add(roleName.toLowerCase());
            }
        }
        return result;
    }

    private TokenGrantResponse requestToken(MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                realmTokenUrl(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                Map.class
        );

        Map<?, ?> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Keycloak returned empty token response");
        }

        return new TokenGrantResponse(
                stringVal(body.get("access_token")),
                stringVal(body.get("refresh_token")),
                longVal(body.get("expires_in"), 0L)
        );
    }

    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);

        return requestToken(form).accessToken();
    }

    private void assignRealmRole(String adminToken, String userId, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        try {
            ResponseEntity<Map> roleResponse = restTemplate.exchange(
                    realmAdminRolesUrl() + "/" + roleName,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<?, ?> roleBody = roleResponse.getBody();
            if (roleBody == null) {
                return;
            }

            Map<String, Object> roleRep = Map.of(
                    "id", stringVal(roleBody.get("id")),
                    "name", stringVal(roleBody.get("name"))
            );

            HttpHeaders postHeaders = new HttpHeaders();
            postHeaders.setContentType(MediaType.APPLICATION_JSON);
            postHeaders.setBearerAuth(adminToken);

            restTemplate.exchange(
                    realmAdminUsersUrl() + "/" + userId + "/role-mappings/realm",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(roleRep), postHeaders),
                    Void.class
            );
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                log.warn(
                        "Keycloak realm role '{}' not found or role-mapping failed with 404; user {} left without that realm role",
                        roleName,
                        userId);
                return;
            }
            throw e;
        }
    }

    private String realmTokenUrl() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    private String realmAdminUsersUrl() {
        return authServerUrl + "/admin/realms/" + realm + "/users";
    }

    private String realmAdminRolesUrl() {
        return authServerUrl + "/admin/realms/" + realm + "/roles";
    }

    private static String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long longVal(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? defaultValue : Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public record TokenGrantResponse(String accessToken, String refreshToken, long expiresIn) {
    }

    public record KeycloakUserProfile(
            String id,
            String email,
            String username,
            String firstName,
            String lastName,
            Boolean emailVerified,
            List<String> realmRoles
    ) {
    }
}
