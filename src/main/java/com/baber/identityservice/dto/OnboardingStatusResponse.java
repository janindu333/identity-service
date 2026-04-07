package com.baber.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for onboarding status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusResponse {
    /**
     * Map of step keys to completion status
     * e.g., {"email_verification": true, "salon_creation": false}
     */
    private Map<String, Boolean> completedSteps;
    
    /**
     * Current step the user is on
     */
    private String currentStep;
    
    /**
     * Next required step that needs to be completed
     */
    private String nextRequiredStep;
    
    /**
     * Percentage of required steps completed (0-100)
     */
    private Integer completionPercentage;
    
    /**
     * Whether all required steps are completed
     */
    private Boolean isOnboardingComplete;
    
    /**
     * Total number of required steps
     */
    private Integer totalRequiredSteps;
    
    /**
     * Number of completed required steps
     */
    private Integer completedRequiredSteps;
}
