package com.baber.identityservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogoutResponse {

    /**
     * Browser URL to end the Keycloak SSO session (and federated Google session when configured).
     * Frontend may redirect or open in a hidden iframe after clearing local tokens.
     */
    private String keycloakLogoutUrl;
}
