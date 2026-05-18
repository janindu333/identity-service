package com.baber.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleCallbackRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String redirectUri;

    /** Optional OAuth state returned from /auth/google/authorize. */
    private String state;
}
