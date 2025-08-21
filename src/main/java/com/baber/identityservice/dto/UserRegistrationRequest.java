package com.baber.identityservice.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class UserRegistrationRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;

    private Double latitude;
    private Double longitude;

    private Long roleId; // Changed from String role to Long roleId
}
