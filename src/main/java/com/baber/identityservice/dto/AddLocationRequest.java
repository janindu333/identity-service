package com.baber.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddLocationRequest {
    private Double latitude;
    private Double longitude;
    private Long userId;
}
