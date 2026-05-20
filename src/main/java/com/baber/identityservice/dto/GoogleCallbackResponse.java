package com.baber.identityservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoogleCallbackResponse {

    private Boolean requiresRoleSelection;
    private String registrationToken;
    private String email;
    private Boolean existingAccount;

    private String accessToken;
    private Long expiresIn;
    private LoginUserResponse user;
    private OnboardingStatusResponse onboardingStatus;

    public static GoogleCallbackResponse roleSelectionRequired(String registrationToken, String email) {
        GoogleCallbackResponse response = new GoogleCallbackResponse();
        response.setRequiresRoleSelection(true);
        response.setRegistrationToken(registrationToken);
        response.setEmail(email);
        return response;
    }

    public static GoogleCallbackResponse fromLogin(TokenResponse tokenResponse, boolean existingAccount) {
        GoogleCallbackResponse response = new GoogleCallbackResponse();
        response.setExistingAccount(existingAccount);
        if (tokenResponse == null) {
            return response;
        }
        response.setAccessToken(tokenResponse.getAccessToken());
        response.setExpiresIn(tokenResponse.getExpiresIn());
        response.setUser(tokenResponse.getUser());
        response.setOnboardingStatus(tokenResponse.getOnboardingStatus());
        return response;
    }
}
