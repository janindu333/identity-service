package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/permissions")
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

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

    @GetMapping("/salon/{salonId}/role/{roleName}")
    public BaseResponse<Set<Permission>> getPermissionsBySalonAndRole(
            @PathVariable Long salonId,
            @PathVariable String roleName) {
        try {
            Set<Permission> permissions = permissionService.getPermissionsBySalonAndRole(salonId, roleName);
            return new BaseResponse<>(true, "Permissions retrieved", 0, null,
                    permissions);
        } catch (RuntimeException e) {
            return new BaseResponse<>(false, "", 404, e.getMessage(), null);
        }
    }

    // Update custom permissions for a specific salon and role
    @PutMapping("/salon/{salonId}/role/{roleName}")
    public BaseResponse<List<Permission>> updatePermissionsBySalonAndRole(
            @PathVariable Long salonId,
            @PathVariable String roleName,
            @RequestBody List<Permission> permissions) {
        List<Permission> updatedPermissions = permissionService.updatePermissionsBySalonAndRole(salonId,
                roleName, permissions);
        return new BaseResponse<>(true, "Permissions updated", 0, null, updatedPermissions);
    }

    @PostMapping("/add")
    public BaseResponse<List<Permission>> createPermissions(@RequestBody List<Permission> permissions) {
        List<Permission> savedPermissions = permissions.stream()
                .filter(permission -> {
                    Optional<Permission> existingPermission = permissionService.findByName(permission.getName());
                    return existingPermission.isEmpty();
                })
                .map(permissionService::save)
                .collect(Collectors.toList());

        if (savedPermissions.isEmpty()) {
            return new BaseResponse<>(false, "", 0, "All permissions already exist", null);
        } else {
            return new BaseResponse<>(true, "Permissions processed", 0, null, savedPermissions);
        }
    }

    // Add a permission to a specific salon and role
    @PostMapping("/salon/{salonId}/role/{roleName}/add")
    public BaseResponse<List<Permission>> addPermissionToRole(
            @PathVariable Long salonId,
            @PathVariable String roleName,
            @RequestBody Permission permission) {
        List<Permission> updatedPermissions = permissionService.addPermissionToRole(salonId, roleName, permission);
        return new BaseResponse<>(true, "Permission added successfully", 0, null,
                updatedPermissions);
    }

    // Remove a permission from a specific salon and role
    @DeleteMapping("/salon/{salonId}/role/{roleId}/permission/{permissionId}/remove")
    public BaseResponse<List<Permission>> removePermissionFromRole(
            @PathVariable Long salonId,
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        List<Permission> updatedPermissions = permissionService.removePermissionFromRole(salonId, roleId,
                permissionId);

        if (updatedPermissions == null) {
            return new BaseResponse<>(false, null,
                    1, "Permission does not exist for this role and salon.", null);
        }

        return new BaseResponse<>(true, "Permission removed successfully", 0, null,
                updatedPermissions);
    }
}
