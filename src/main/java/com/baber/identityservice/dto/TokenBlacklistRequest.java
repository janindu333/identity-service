package com.baber.identityservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token blacklist request")
public class TokenBlacklistRequest {
    
    @Schema(description = "JWT token to blacklist", example = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRva2VuVHlwZSI6IkFDQ0VTUyIsInN1YiI6ImphbmluZHUiLCJpYXQi0jE3NTQwNDgzMzYsImV4cCI6MTc1NDA1NTUzNn0.nSnBu6hiEwHb0cWWsRzhe3h7jiW6SJ9wf4_NW06tU7A", required = true)
    private String token;
    
    @Schema(description = "Username associated with the token", example = "janindu", required = true)
    private String username;
    
    @Schema(description = "Reason for blacklisting", example = "User logout", required = false)
    private String reason;
    
    @Schema(description = "Hours to keep token blacklisted", example = "2", required = false, defaultValue = "2")
    private Integer expirationHours = 2;
} 