package com.baber.identityservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;

    private Double longitude;

    /**
     * Stable Keycloak user identifier (JWT "sub").
     * In phase-1 migration this becomes the primary identity linkage
     * while legacy fields are still kept for compatibility.
     */
    @Column(length = 64, unique = true)
    private String keycloakUserId;
    
    /**
     * Onboarding status - JSON string storing completed steps
     * Format: {"email_verification": true, "salon_creation": false, ...}
     * Only relevant for owners - defaults to empty for other roles
     */
    @Column(columnDefinition = "TEXT")
    private String onboardingStatus = "{}";
    
    /**
     * Current onboarding step the user is on (for owners)
     * Null or empty means onboarding not started or completed
     */
    @Column(length = 50)
    private String currentOnboardingStep;

    @Column(nullable = false)
    private Boolean emailVerified = false;

}
