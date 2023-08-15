package com.baber.identityservice.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class AddLocationRequest {
    private String latitude;
    private String longitude;
    private int userId;
}
