package com.baber.identityservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
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
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;

    private Double latitude;

    private Double longitude;
    
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    
    @Column(nullable = false)
    private Boolean emailVerified = false;
    
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

    // Convenience method to get full name
    public String getName() {
        return firstName + " " + lastName;
    }

    // Convenience method to set name (for backward compatibility)
    public void setName(String name) {
        String[] parts = name.split(" ", 2);
        this.firstName = parts[0];
        this.lastName = parts.length > 1 ? parts[1] : "";
    }

}
