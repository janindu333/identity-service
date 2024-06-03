package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public BaseResponse<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return new BaseResponse<>(true, "Roles retrieved successfully", 0, null, roles);
    }

    @GetMapping("/{id}")
    public BaseResponse<Role> getRoleById(@PathVariable int id) {
        Optional<Role> role = roleService.getRoleById(id);
        return role.map(value -> new BaseResponse<>(true, "Role retrieved successfully", 0, null, value))
                .orElseGet(() -> new BaseResponse<>(false, "Role not found", 404, "Role with id " + id + " not found", null));
    }

    @PostMapping("/add")
    public BaseResponse<Role> createRole(@RequestBody Role role) {
        return roleService.createRole(role);
    }

    @PutMapping("/{id}")
    public BaseResponse<Role> updateRole(@PathVariable int id, @RequestBody Role roleDetails) {
        Optional<Role> updatedRole = roleService.updateRole(id, roleDetails);
        return updatedRole.map(value -> new BaseResponse<>(true, "Role updated successfully", 0, null, value))
                .orElseGet(() -> new BaseResponse<>(false, "Role not found", 404, "Role with id " + id + " not found", null));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteRole(@PathVariable int id) {
        boolean isDeleted = roleService.deleteRole(id);
        if (isDeleted) {
            return new BaseResponse<>(true, "Role deleted successfully", 0, null, null);
        } else {
            return new BaseResponse<>(false, "Role not found", 404, "Role with id " + id + " not found", null);
        }
    }
}
