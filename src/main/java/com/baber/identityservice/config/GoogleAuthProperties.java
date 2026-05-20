package com.baber.identityservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.google")
public class GoogleAuthProperties {

    /** When false, Google endpoints return 503. */
    private boolean enabled = false;

    /** Google OAuth client ID (Web) — must match the {@code aud} claim on Google ID tokens. */
    private String clientId = "";

    /** Allowed redirect URIs for the authorization-code callback (comma-separated). */
    private String allowedRedirectUris = "";

    /** Default redirect when the client omits {@code redirectUri} on /authorize. */
    private String defaultRedirectUri = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAllowedRedirectUris() {
        return allowedRedirectUris;
    }

    public void setAllowedRedirectUris(String allowedRedirectUris) {
        this.allowedRedirectUris = allowedRedirectUris;
    }

    public String getDefaultRedirectUri() {
        return defaultRedirectUri;
    }

    public void setDefaultRedirectUri(String defaultRedirectUri) {
        this.defaultRedirectUri = defaultRedirectUri;
    }
}
