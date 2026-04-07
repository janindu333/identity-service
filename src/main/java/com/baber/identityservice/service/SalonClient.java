package com.baber.identityservice.service;

import com.baber.identityservice.config.ServiceLogger;
import com.baber.identityservice.dto.OwnerSalonSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Client for communicating with saloon-service.
 *
 * Currently used to determine whether an owner already has a salon
 * and to fetch its publicId for login/onboarding flows.
 */
@Service
public class SalonClient {

    private static final ServiceLogger logger = new ServiceLogger(SalonClient.class);

    private final RestTemplate restTemplate;

    @Autowired
    public SalonClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get the publicId of the first salon owned by the given owner.
     *
     * @param ownerId owner user ID from identity-service
     * @return saloonId as a String, or null if none found or on error
     */
    public String getSalonPublicIdForOwner(Long ownerId) {
        try {
            // Uses Eureka service ID "SALOON-SERVICE" (matches existing config)
            String url = "http://SALOON-SERVICE/api/saloon/owner/{ownerId}/summary";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class, ownerId);

            if (response == null) {
                logger.warn("SalonClient: null response when fetching salon for ownerId=" + ownerId);
                return null;
            }

            Object successObj = response.get("success");
            if (!(successObj instanceof Boolean) || !((Boolean) successObj)) {
                logger.info("SalonClient: no salon for ownerId=" + ownerId + " or request not successful");
                return null;
            }

            Object dataObj = response.get("data");
            if (!(dataObj instanceof Map<?, ?> dataMap)) {
                return null;
            }

            Object saloonIdObj = dataMap.get("saloonId");
            return saloonIdObj != null ? saloonIdObj.toString() : null;
        } catch (Exception e) {
            logger.warn("SalonClient: failed to fetch salon for ownerId=" + ownerId + ", error=" + e.getMessage());
            return null;
        }
    }

    /**
     * Get full owner salon summary including onboarding completion flags.
     * Used by OnboardingService to sync business_hours, services_setup, staff_invitation from saloon-service.
     */
    public Optional<OwnerSalonSummaryDto> getOwnerSalonSummary(Long ownerId) {
        try {
            String url = "http://SALOON-SERVICE/api/saloon/owner/{ownerId}/summary";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class, ownerId);

            if (response == null) {
                return Optional.empty();
            }

            Object successObj = response.get("success");
            if (!(successObj instanceof Boolean) || !((Boolean) successObj)) {
                return Optional.empty();
            }

            Object dataObj = response.get("data");
            if (!(dataObj instanceof Map<?, ?> dataMap)) {
                return Optional.empty();
            }

            Object saloonIdObj = dataMap.get("saloonId");
            String saloonId = saloonIdObj != null ? saloonIdObj.toString() : null;
            Boolean hasBusinessHours = asBoolean(dataMap.get("hasBusinessHours"));
            Boolean hasServices = asBoolean(dataMap.get("hasServices"));
            Boolean hasStaffInvite = asBoolean(dataMap.get("hasStaffInvite"));

            return Optional.of(new OwnerSalonSummaryDto(saloonId, hasBusinessHours, hasServices, hasStaffInvite));
        } catch (Exception e) {
            logger.warn("SalonClient: failed to fetch owner salon summary for ownerId=" + ownerId + ", error=" + e.getMessage());
            return Optional.empty();
        }
    }

    private static Boolean asBoolean(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean b) return b;
        if (obj instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}

