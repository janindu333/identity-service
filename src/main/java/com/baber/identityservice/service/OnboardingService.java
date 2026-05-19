package com.baber.identityservice.service;

import com.baber.identityservice.dto.OnboardingStatusResponse;
import com.baber.identityservice.entity.UserCredential;
import com.baber.identityservice.enums.OnboardingStep;
import com.baber.identityservice.repository.UserCredentialRepository;
import com.baber.identityservice.dto.OwnerSalonSummaryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing user onboarding status
 * Based on industry best practices for multi-step onboarding flows
 */
@Service
public class OnboardingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private SalonClient salonClient;
    
    /**
     * Get onboarding status for a user
     */
    public OnboardingStatusResponse getOnboardingStatus(Long userId) {
        return getOnboardingStatus(userId, null, null, null);
    }

    /**
     * Get onboarding status for a user, optionally using Keycloak-sourced
     * identity signals to avoid relying on local auth columns.
     */
    public OnboardingStatusResponse getOnboardingStatus(Long userId, Boolean emailVerifiedOverride, Boolean isOwnerOverride) {
        return getOnboardingStatus(userId, emailVerifiedOverride, isOwnerOverride, null);
    }

    /**
     * @param prefetchedSalonSummary when provided (e.g. from login), avoids a second call to saloon-service
     */
    public OnboardingStatusResponse getOnboardingStatus(
            Long userId,
            Boolean emailVerifiedOverride,
            Boolean isOwnerOverride,
            Optional<OwnerSalonSummaryDto> prefetchedSalonSummary) {
        Optional<UserCredential> userOpt = userCredentialRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        UserCredential user = userOpt.get();
        Map<String, Boolean> completedSteps = parseOnboardingStatus(user.getOnboardingStatus());
        
        // Auto-complete email verification if user's email is verified
        boolean emailVerified = emailVerifiedOverride != null ? emailVerifiedOverride : false;
        if (emailVerified && !completedSteps.getOrDefault("email_verification", false)) {
            completedSteps.put("email_verification", true);
            // Save updated status
            saveOnboardingStatus(user, completedSteps);
        }

        // If this is an owner, sync onboarding steps from saloon-service (salon exists, business hours, services, staff invite).
        try {
            boolean isOwner = isOwnerOverride != null ? isOwnerOverride : false;

            if (isOwner) {
                Optional<OwnerSalonSummaryDto> summaryOpt = prefetchedSalonSummary != null
                        ? prefetchedSalonSummary
                        : salonClient.getOwnerSalonSummary(user.getId());
                if (summaryOpt.isPresent()) {
                    OwnerSalonSummaryDto summary = summaryOpt.get();
                    boolean updated = false;
                    if (summary.getSaloonId() != null && !completedSteps.getOrDefault("salon_creation", false)) {
                        completedSteps.put("salon_creation", true);
                        updated = true;
                    }
                    if (Boolean.TRUE.equals(summary.getHasBusinessHours()) && !completedSteps.getOrDefault("business_hours", false)) {
                        completedSteps.put("business_hours", true);
                        updated = true;
                    }
                    if (Boolean.TRUE.equals(summary.getHasServices()) && !completedSteps.getOrDefault("services_setup", false)) {
                        completedSteps.put("services_setup", true);
                        updated = true;
                    }
                    if (Boolean.TRUE.equals(summary.getHasStaffInvite()) && !completedSteps.getOrDefault("staff_invitation", false)) {
                        completedSteps.put("staff_invitation", true);
                        updated = true;
                    }
                    if (Boolean.TRUE.equals(summary.getHasPaymentSetup()) && !completedSteps.getOrDefault("payment_setup", false)) {
                        completedSteps.put("payment_setup", true);
                        updated = true;
                    }
                    if (updated) {
                        saveOnboardingStatus(user, completedSteps);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to sync onboarding steps from saloon-service for userId={}", userId, e);
        }

        normalizeCompletedStepsMap(completedSteps);

        // Calculate statistics
        int totalRequired = 0;
        int completedRequired = 0;
        String nextRequiredStep = null;
        
        for (OnboardingStep step : OnboardingStep.values()) {
            if (step.isRequired()) {
                totalRequired++;
                boolean completed = completedSteps.getOrDefault(step.getKey(), false);
                if (completed) {
                    completedRequired++;
                } else if (nextRequiredStep == null) {
                    nextRequiredStep = step.getKey();
                }
            }
        }
        
        int completionPercentage = totalRequired > 0 
            ? (int) Math.round((double) completedRequired / totalRequired * 100) 
            : 100;
        
        boolean isComplete = completedRequired == totalRequired;
        
        // Always derive current step from completed steps (avoid stale "salon_creation" after login sync).
        String currentStep;
        if (isComplete) {
            currentStep = "complete";
        } else {
            currentStep = nextRequiredStep != null ? nextRequiredStep : "email_verification";
        }
        if (!currentStep.equals(user.getCurrentOnboardingStep())) {
            user.setCurrentOnboardingStep(currentStep);
            userCredentialRepository.save(user);
        }
        
        OnboardingStatusResponse response = new OnboardingStatusResponse();
        response.setCompletedSteps(completedSteps);
        response.setCurrentStep(currentStep);
        response.setNextRequiredStep(nextRequiredStep);
        response.setCompletionPercentage(completionPercentage);
        response.setIsOnboardingComplete(isComplete);
        response.setTotalRequiredSteps(totalRequired);
        response.setCompletedRequiredSteps(completedRequired);
        
        return response;
    }
    
    /**
     * Mark an onboarding step as complete
     */
    @Transactional
    public void completeOnboardingStep(Long userId, String stepKey) {
        Optional<UserCredential> userOpt = userCredentialRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for onboarding step completion: userId={}", userId);
            return;
        }
        
        UserCredential user = userOpt.get();
        Map<String, Boolean> completedSteps = parseOnboardingStatus(user.getOnboardingStatus());
        
        // Validate step key
        OnboardingStep step = OnboardingStep.fromKey(stepKey);
        if (step == null) {
            logger.warn("Invalid onboarding step key: {}", stepKey);
            return;
        }
        
        // Mark step as complete
        completedSteps.put(stepKey, true);
        
        // Update current step to next incomplete step
        String nextStep = findNextIncompleteStep(completedSteps);
        user.setCurrentOnboardingStep(nextStep);
        
        // Save updated status
        saveOnboardingStatus(user, completedSteps);
        
        logger.info("Onboarding step completed: userId={}, step={}", userId, stepKey);
    }
    
    /**
     * Mark email verification step as complete
     */
    @Transactional
    public void completeEmailVerification(Long userId) {
        completeOnboardingStep(userId, "email_verification");
    }
    
    /**
     * Mark salon creation step as complete
     */
    @Transactional
    public void completeSalonCreation(Long userId) {
        completeOnboardingStep(userId, "salon_creation");
    }
    
    /**
     * Every defined onboarding step appears explicitly in API responses (true/false).
     * Adds {@code staff_invite} as an alias of {@code staff_invitation} for older clients.
     * Drops removed/legacy keys (e.g. {@code profile_completion}).
     */
    private void normalizeCompletedStepsMap(Map<String, Boolean> completedSteps) {
        Map<String, Boolean> normalized = new HashMap<>();
        for (OnboardingStep step : OnboardingStep.values()) {
            normalized.put(step.getKey(), Boolean.TRUE.equals(completedSteps.get(step.getKey())));
        }
        boolean staffDone = Boolean.TRUE.equals(completedSteps.get("staff_invitation"))
                || Boolean.TRUE.equals(completedSteps.get("staff_invite"));
        normalized.put("staff_invitation", staffDone);
        normalized.put("staff_invite", staffDone);
        completedSteps.clear();
        completedSteps.putAll(normalized);
    }

    /**
     * Parse onboarding status JSON string to Map
     */
    private Map<String, Boolean> parseOnboardingStatus(String statusJson) {
        try {
            if (statusJson == null || statusJson.trim().isEmpty() || statusJson.equals("{}")) {
                return new HashMap<>();
            }
            return objectMapper.readValue(statusJson, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse onboarding status JSON: {}", statusJson, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Save onboarding status to user entity
     */
    private void saveOnboardingStatus(UserCredential user, Map<String, Boolean> completedSteps) {
        try {
            String statusJson = objectMapper.writeValueAsString(completedSteps);
            user.setOnboardingStatus(statusJson);
            userCredentialRepository.save(user);
        } catch (Exception e) {
            logger.error("Failed to save onboarding status", e);
        }
    }
    
    /**
     * Find next incomplete required step
     */
    private String findNextIncompleteStep(Map<String, Boolean> completedSteps) {
        for (OnboardingStep step : OnboardingStep.values()) {
            if (step.isRequired() && !completedSteps.getOrDefault(step.getKey(), false)) {
                return step.getKey();
            }
        }
        return "complete";
    }
    
    /**
     * Check if user can access main features (onboarding complete)
     */
    public boolean isOnboardingComplete(Long userId) {
        OnboardingStatusResponse status = getOnboardingStatus(userId);
        return status != null && status.getIsOnboardingComplete();
    }
}
