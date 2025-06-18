package com.baber.identityservice.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddLocationRequest {
    private String latitude;
    private String longitude;
    private int userId;
}
