package com.baber.identityservice.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor

public class TokenResponse {

    private String accessToken;
    private String refreshToken;

   private String role;
}
