package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.repository.PermissionRepository;
import com.baber.identityservice.identityservice.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PermissionService {
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    public List<Permission> getDefaultPermissions(Long roleId) {
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            return role.getPermissions();
        } else {
            // Return null to indicate that the role was not found
            return null;
        }
    }

    public Set<Permission> getPermissionsBySalonAndRole(Long salonId, String roleName) {
        // Validate if the role exists for the given salon
        Optional<Role> roleOptional = roleRepository.findByNameAndSalonId(roleName, salonId);
        if (roleOptional.isEmpty()) {
            throw new RuntimeException("role called " + roleName +  " does not found for the given salon");
        }

        // Fetch permissions
        Set<Permission> permissions = permissionRepository.findBySalonAndRole(salonId, roleName);
        if (permissions.isEmpty()) {
            throw new RuntimeException("No permissions found for the given role");
        }

        return permissions;
    }

    public List<Permission> updatePermissionsBySalonAndRole(Long salonId, String roleName, List<Permission> permissions)
    {
        // Fetch the role
        Role role = roleRepository.findByNameAndSalonId(roleName, salonId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Update permissions
        role.setPermissions(permissions);
        roleRepository.save(role);

        return role.getPermissions();
    }

    public Permission save(Permission permission) {
        return permissionRepository.save(permission);
    }
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }

    public List<Permission> addPermissionToRole(Long salonId, String roleName, Permission permission) {
        Role role = roleRepository.findByNameAndSalonId(roleName, salonId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        role.getPermissions().add(permission);
        roleRepository.save(role);
        return role.getPermissions();
    }

    public List<Permission> removePermissionFromRole(Long salonId, Long roleId, Long permissionId) {
        Role role = roleRepository.findByIdAndSalonId(roleId, salonId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        boolean permissionExists = role.getPermissions().stream()
                .anyMatch(existingPermission -> existingPermission.getId().equals(permissionId));

        if (!permissionExists) {
            return null; // Return null or an empty list if the permission does not exist
        }

        role.getPermissions().removeIf(existingPermission -> existingPermission.getId().equals(permissionId));
        roleRepository.save(role);
        return new ArrayList<>(role.getPermissions());
    }

}

