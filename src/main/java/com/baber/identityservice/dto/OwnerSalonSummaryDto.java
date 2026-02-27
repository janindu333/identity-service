package com.baber.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of an owner's salon from saloon-service, used to sync onboarding status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSalonSummaryDto {
    private String saloonId;
    private Boolean hasBusinessHours;
    private Boolean hasServices;
    private Boolean hasStaffInvite;
}
