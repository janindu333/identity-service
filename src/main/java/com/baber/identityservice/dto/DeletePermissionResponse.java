package com.baber.identityservice.dto;

import com.baber.identityservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeletePermissionResponse {
    private Long permissionId;
    private String permissionName;
    private boolean deleted;
    private List<Role> affectedRoles;
    private String message;
} 