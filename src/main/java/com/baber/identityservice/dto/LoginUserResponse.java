package com.baber.identityservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight user info returned as part of the login response.
 *
 * Mirrors the frontend contract, for example:
 * {
 *   "id": 15,
 *   "fullName": "janindu praneeth",
 *   "role": "owner",
 *   "saloonId": "a2f5c1c0-1234-5678-9999-abcdefabcdef",
 *   "salonStatus": "pending_setup"
 * }
 *
 * Optional fields like saloonId and salonStatus will be omitted
 * from the JSON when null.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginUserResponse {

    /**
     * Internal user ID from identity-service.
     */
    private Long id;

    /**
     * Full display name for the user.
     */
    private String fullName;

    /**
     * Normalized role name (e.g. \"owner\").
     */
    private String role;

    /**
     * Public salon identifier (UUID string) when the owner already
     * has a salon. Null when no salon exists yet.
     */
    private String saloonId;

    /**
     * High level salon status, for example:
     * - \"pending_setup\"
     * - \"active\"
     *
     * This is only populated when saloonId is present.
     */
    private String salonStatus;
}

