package com.baber.identityservice.enums;

/**
 * Enum representing onboarding steps for salon owners
 * Based on industry best practices for multi-step onboarding flows
 */
public enum OnboardingStep {
    /**
     * Step 1: Email verification (required)
     */
    EMAIL_VERIFICATION("email_verification", "Email Verification", true),
    
    /**
     * Step 2: Salon creation (required for owners)
     */
    SALON_CREATION("salon_creation", "Create Your Salon", true),
    
    /**
     * Step 3: Business hours setup (required for owners)
     */
    BUSINESS_HOURS("business_hours", "Set Business Hours", true),
    
    /**
     * Step 4: Services setup (required for owners)
     */
    SERVICES_SETUP("services_setup", "Add Services", true),
    
    /**
     * Step 5: Staff invitation (required)
     */
    STAFF_INVITATION("staff_invitation", "Invite Staff Members", true),
    
    /**
     * Step 6: Payment setup (optional)
     */
    PAYMENT_SETUP("payment_setup", "Configure Payment Methods", false),
    
    /**
     * Step 7: Profile completion (optional)
     */
    PROFILE_COMPLETION("profile_completion", "Complete Your Profile", false);

    private final String key;
    private final String displayName;
    private final boolean required;

    OnboardingStep(String key, String displayName, boolean required) {
        this.key = key;
        this.displayName = displayName;
        this.required = required;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequired() {
        return required;
    }

    public static OnboardingStep fromKey(String key) {
        for (OnboardingStep step : values()) {
            if (step.key.equals(key)) {
                return step;
            }
        }
        return null;
    }
}
