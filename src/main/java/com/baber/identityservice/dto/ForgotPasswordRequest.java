package com.baber.identityservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordRequest {

    // Used for requesting a password reset link
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    // Used for legacy reset endpoint that resets by userId + newPassword
    // (AuthController.reset-password calls getUserId() and getNewPassword())
    private Long userId;

    private String newPassword;
}
