package com.baber.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateTokenResponse {
    private boolean valid;
    private String message;
    private Long expiresInMinutes; // Time remaining until expiration
}

