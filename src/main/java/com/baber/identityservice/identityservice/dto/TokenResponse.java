package com.baber.identityservice.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class TokenResponse {

    private String accessToken;
    private String refreshToken;
}
