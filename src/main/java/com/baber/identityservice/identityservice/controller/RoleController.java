package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.service.PermissionService;
import com.baber.identityservice.identityservice.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/roles")
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
    public BaseResponse<Role> getRoleById(@PathVariable int id) {
        Optional<Role> role = roleService.getRoleById(id);
        return role.map(value -> new BaseResponse<>(true, "Role retrieved successfully", 0,
                        null, value))
                .orElseGet(() -> new BaseResponse<>(false, "Role not found", 404,
                        "Role with id " + id + " not found", null));
    }

    @PostMapping("/add")
    public BaseResponse<Role> createRole(@RequestBody Role role) {
        Optional<Role> createdRole = roleService.createRole(role);
        return createdRole.map(value -> new BaseResponse<>(true, "Role created successfully", 0,
                        null, value))
                .orElseGet(() -> new BaseResponse<>(false, "Role already exists", 409,
                        "Role with name " + role.getName() + " already exists", null));
    }

    @PutMapping("/{id}")
    public BaseResponse<Role> updateRole(@PathVariable int id, @RequestBody Role roleDetails) {
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

    @GetMapping("/salon/{salonId}/role/{roleName}")
    public ResponseEntity<Role> getRoleByNameAndSalonId(@PathVariable Long salonId, @PathVariable String roleName) {
        return roleService.findByNameAndSalonId(roleName, salonId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{roleId}/permissions")
    public BaseResponse<Role> updateRolePermissions(@PathVariable Long roleId, @RequestBody
    List<Permission> permissions) {
        Role role = roleService.getRoleById(Math.toIntExact(roleId)).orElseThrow(() -> new RuntimeException("Role not found"));
        role.setPermissions(permissions);
        roleService.createRole(role);
        return new BaseResponse<>(true, "Role update successfully", 0, null,
                null);
    }
}
