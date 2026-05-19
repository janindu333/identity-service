package com.baber.identityservice.dto;

import lombok.Data;

@Data
public class GoogleCallbackRequest {
    /** Authorization code from Keycloak (first step). Not required when {@link #registrationToken} is set. */
    private String code;

    private String redirectUri;

    /** Optional OAuth state returned from /auth/google/authorize. */
    private String state;

    /** Owner ({@code 3}) or customer ({@code 7}) — required on second step for new users. */
    private Long role;

    /** Opaque token from first callback when role selection is required. */
    private String registrationToken;
}
