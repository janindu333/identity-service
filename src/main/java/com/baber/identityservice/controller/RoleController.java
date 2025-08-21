package com.baber.identityservice.controller;

import com.baber.identityservice.dto.AddRoleRequest;
import com.baber.identityservice.dto.AssignPermissionsRequest;
import com.baber.identityservice.dto.BaseResponse;
import com.baber.identityservice.dto.GetRoleByIdResponse;
import com.baber.identityservice.dto.RemovePermissionRequest;
import com.baber.identityservice.dto.UpdateRoleRequest;
import com.baber.identityservice.entity.Permission;
import com.baber.identityservice.entity.Role;
import com.baber.identityservice.service.PermissionService;
import com.baber.identityservice.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/roles")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public BaseResponse<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return new BaseResponse<>(true, "Roles retrieved successfully", 0,
                null, roles);
    }

    @GetMapping("/{id}")
    public BaseResponse<GetRoleByIdResponse> getRoleById(@PathVariable int id) {
        Optional<Role> roleOpt = roleService.getRoleById(id);
        return roleOpt.map(role -> {
            GetRoleByIdResponse dto = new GetRoleByIdResponse(); 
            dto.setName(role.getName());
            dto.setDescription(role.getDescription()); 
            // Map permissions as needed
            dto.setPermissions(
                role.getPermissions() == null ? List.of() :
                role.getPermissions().stream()
                    .map(Permission::getName) // or another field
                    .collect(Collectors.toList())
            );
            return new BaseResponse<>(true, "Role retrieved successfully", 0, null, dto);
        }).orElseGet(() -> new BaseResponse<>(false, "Role not found", 404, "Role with id " + id + " not found", null));
    }

    @PostMapping("/add")
    public BaseResponse<Role> createRole(@RequestBody AddRoleRequest role) {
        Optional<Role> createdRole = roleService.createRole(role);
        return createdRole.map(value -> new BaseResponse<>(true, "Role created successfully", 0,
                        null, value))
                .orElseGet(() -> new BaseResponse<>(false, "Role already exists", 409,
                        "Role with name " + role.getName() + " already exists", null));
    }

    @PutMapping("/{id}")
    public BaseResponse<Role> updateRole(@PathVariable int id, @RequestBody UpdateRoleRequest roleDetails) {
        Optional<Role> updatedRole = roleService.updateRole(id, roleDetails);
        return updatedRole.map(value -> new BaseResponse<>(true, "Role updated successfully",
                        0, null, value))
                .orElseGet(() -> new BaseResponse<>(false, "Role not found", 404,
                        "Role with id " + id + " not found", null));
    }

    @DeleteMapping("/{id}/remove")
    public BaseResponse<Void> deleteRole(@PathVariable int id) {
        boolean isDeleted = roleService.deleteRole(id);
        if (isDeleted) {
            return new BaseResponse<>(true, "Role deleted successfully", 0, null,
                    null);
        } else {
            return new BaseResponse<>(false, "Role not found", 404,
                    "Role with id " + id + " not found", null);
        }
    }

    @PutMapping("/{roleId}/permissions")
    public BaseResponse<Role> assignPermissionsToRole(
            @PathVariable int roleId,
            @RequestBody AssignPermissionsRequest request) {
        
        if (request.getPermissionIds() == null || request.getPermissionIds().isEmpty()) {
            return new BaseResponse<>(false, "No permissions provided", 400,
                    "Permission IDs list cannot be null or empty", null);
        }

        Optional<Role> updatedRole = roleService.assignPermissionsToRole(roleId, request.getPermissionIds());
        
        return updatedRole.map(role -> new BaseResponse<>(true, "Permissions assigned successfully", 0,
                        null, role))
                .orElseGet(() -> new BaseResponse<>(false, "Role not found", 404,
                        "Role with id " + roleId + " not found", null));
    }

    @DeleteMapping("/{roleId}/permissions")
    public BaseResponse<Role> removePermissionFromRole(
            @PathVariable int roleId,
            @RequestBody RemovePermissionRequest request) {
        
        if (request.getPermissionId() == null) {
            return new BaseResponse<>(false, "No permission provided", 400,
                    "Permission ID cannot be null", null);
        }

        Optional<Role> updatedRole = roleService.removePermissionFromRole(roleId, request.getPermissionId());
        
        return updatedRole.map(role -> new BaseResponse<>(true, "Permission removed successfully", 0,
                        null, role))
                .orElseGet(() -> new BaseResponse<>(false, "Role or permission not found", 404,
                        "Role with id " + roleId + " not found or permission not assigned to this role", null));
    }

}
