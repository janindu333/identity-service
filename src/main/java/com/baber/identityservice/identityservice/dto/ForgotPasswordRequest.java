package com.baber.identityservice.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class ForgotPasswordRequest {

    private String newPassword;
    private int id;
}
