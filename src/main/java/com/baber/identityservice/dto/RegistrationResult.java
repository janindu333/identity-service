package com.baber.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal result object for user registration.
 * The controller is responsible for mapping this to BaseResponse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResult {

    private boolean success;

    /**
     * Success message (for successful registrations).
     */
    private String message;

    /**
     * Error code for failures (e.g. 400, 404, 409).
     */
    private int errorCode;

    /**
     * Descriptive error message for failures.
     */
    private String errorMessage;
}

