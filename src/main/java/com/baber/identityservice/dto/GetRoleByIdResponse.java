package com.baber.identityservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetRoleByIdResponse {
  
    private String name;
    private String description;
    private List<String> permissions; // or List<PermissionDto> if you want more details
  

    // Constructors, getters, setters
}
