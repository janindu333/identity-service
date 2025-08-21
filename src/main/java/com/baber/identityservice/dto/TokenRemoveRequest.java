package com.baber.identityservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token removal request")
public class TokenRemoveRequest {
    
    @Schema(description = "JWT token to remove from blacklist", example = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRva2VuVHlwZSI6IkFDQ0VTUyIsInN1YiI6ImphbmluZHUiLCJpYXQi0jE3NTQzMDUyMDEsImV4cCI6MTc1NDMxMjQwMX0.a7CAGBLOTvMmZI3VdCMFbs9oUIfk2SzBFxtcmSFJ8JI", required = true)
    private String token;
} 