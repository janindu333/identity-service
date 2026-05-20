package com.baber.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleSignInRequest {
    @NotBlank
    private String idToken;

    /** Local DB role id for first-time sign-up (defaults to customer). */
    private Long role;

    private Boolean rememberMe;
}
