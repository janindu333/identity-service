package com.baber.identityservice.controller;

import com.baber.identityservice.dto.AddPermissionRequest;
import com.baber.identityservice.dto.BaseResponse;
import com.baber.identityservice.dto.DeletePermissionResponse;
import com.baber.identityservice.dto.PermissionRolesResponse;
import com.baber.identityservice.dto.UpdatePermissionRequest; 
import com.baber.identityservice.entity.Permission;
import com.baber.identityservice.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/permissions")
public class PermissionController {
    
    @Autowired
    private PermissionService permissionService;

    @GetMapping("/all")
    public BaseResponse<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return new BaseResponse<>(true, "All permissions retrieved successfully", 0, null, permissions);
    }

    @GetMapping("/defaults/{roleId}")
    public BaseResponse<List<Permission>> getDefaultPermissions(@PathVariable Long roleId) {
        List<Permission> permissions = permissionService.getDefaultPermissions(roleId);
        if (permissions == null) {
            return new BaseResponse<>(false, null, 404,
                    "Role not found", null);
        }
        return new BaseResponse<>(true, "Default permissions retrieved", 0, null,
                permissions);
    }

    @PostMapping("/add")
    public BaseResponse<List<Permission>> createPermissions(@RequestBody 
    List<AddPermissionRequest> permissionRequests) {
        List<Permission> savedPermissions = permissionRequests.stream()
                .filter(request -> {
                    Optional<Permission> existingPermission = permissionService
                    .findByName(request.getName());
                    return existingPermission.isEmpty();
                })
                .map(request -> {
                    Permission permission = new Permission();
                    permission.setName(request.getName());
                    return permissionService.save(permission);
                })
                .collect(Collectors.toList());

        if (savedPermissions.isEmpty()) {
            return new BaseResponse<>(false, "", 0, "All permissions already exist", null);
        } else {
            return new BaseResponse<>(true, "Permissions processed", 0, null, savedPermissions);
        }
    }

    @DeleteMapping("/{permissionId}")
    public BaseResponse<DeletePermissionResponse> deletePermission(@PathVariable Long permissionId) {
        try {
            DeletePermissionResponse response = permissionService.deletePermission(permissionId);
            return new BaseResponse<>(true, "Permission deleted successfully", 0, null, response);
        } catch (RuntimeException e) {
            return new BaseResponse<>(false, null, 404, e.getMessage(), null);
        }
    }

    @GetMapping("/{permissionId}")
    public BaseResponse<Permission> getPermissionById(@PathVariable Long permissionId) {
        try {
            Optional<Permission> permission = permissionService.getPermissionById(permissionId);
            if (permission.isPresent()) {
                return new BaseResponse<>(true, "Permission retrieved successfully", 0, null, permission.get());
            } else {
                return new BaseResponse<>(false, null, 404, "Permission not found", null);
            }
        } catch (RuntimeException e) {
            return new BaseResponse<>(false, null, 500, e.getMessage(), null);
        }
    }

    @PutMapping("/{permissionId}")
    public BaseResponse<Permission> updatePermission(@PathVariable Long permissionId, 
                                                   @RequestBody UpdatePermissionRequest request) {
        try {
            Permission updatedPermission = permissionService.updatePermission(permissionId, request.getName());
            return new BaseResponse<>(true, "Permission updated successfully", 0, null, updatedPermission);
        } catch (RuntimeException e) {
            return new BaseResponse<>(false, null, 400, e.getMessage(), null);
        }
    }

    @GetMapping("/{permissionId}/roles")
    public BaseResponse<PermissionRolesResponse> getRolesForPermission(@PathVariable Long permissionId) {
        try {
            PermissionRolesResponse response = permissionService.getRolesForPermission(permissionId);
            return new BaseResponse<>(true, "Roles for permission retrieved successfully", 0, null, response);
        } catch (RuntimeException e) {
            return new BaseResponse<>(false, null, 404, e.getMessage(), null);
        }
    }

}
