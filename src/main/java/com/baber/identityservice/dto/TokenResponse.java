package com.baber.identityservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    /**
     * Access token for API authentication
     */
    private String accessToken;
    
    /**
     * Access token expiration time in seconds (e.g., 7200 for 2 hours)
     */
    private Long expiresIn;
    
    /**
     * Lightweight user info for the logged-in user.
     *
     * Example for an owner with no salon yet:
     * {
     *   "id": 15,
     *   "fullName": "janindu praneeth",
     *   "role": "owner"
     * }
     */
    private LoginUserResponse user;
    
    /**
     * Onboarding status (only included for owners)
     * Null for non-owner roles
     */
    private OnboardingStatusResponse onboardingStatus;
    
    /**
     * Constructor for cookie-based authentication
     * Note: Refresh token is sent via HTTP-only cookie for security
     * Onboarding status will be null unless explicitly set
     */
    public TokenResponse(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = null;
        this.onboardingStatus = null;
    }
    
    /**
     * Constructor with onboarding status
     * Used for owners who need onboarding status in login response
     */
    public TokenResponse(String accessToken, Long expiresIn, OnboardingStatusResponse onboardingStatus) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = null;
        this.onboardingStatus = onboardingStatus;
    }

    /**
     * Full constructor used by login flows where we want to include
     * both user info and onboarding status.
     */
    public TokenResponse(String accessToken,
                         Long expiresIn,
                         LoginUserResponse user,
                         OnboardingStatusResponse onboardingStatus) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.onboardingStatus = onboardingStatus;
    }
}
