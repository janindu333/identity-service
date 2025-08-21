package com.baber.identityservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baber.identityservice.dto.DeletePermissionResponse;
import com.baber.identityservice.dto.PermissionRolesResponse;
import com.baber.identityservice.entity.Permission;
import com.baber.identityservice.entity.Role;
import com.baber.identityservice.repository.PermissionRepository;
import com.baber.identityservice.repository.RoleRepository;

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

  
 

    public Permission save(Permission permission) {
        return permissionRepository.save(permission);
    }
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }

   

  

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllActive();
    }

    public Optional<Permission> getPermissionById(Long permissionId) {
        return permissionRepository.findById(permissionId);
    }

    public Permission updatePermission(Long permissionId, String newName) {
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            throw new RuntimeException("Permission not found with ID: " + permissionId);
        }
        
        Permission permission = permissionOpt.get();
        
        // Check if the new name already exists (excluding current permission)
        Optional<Permission> existingPermission = permissionRepository.findByName(newName);
        if (existingPermission.isPresent() && !existingPermission.get().getId().equals(permissionId)) {
            throw new RuntimeException("Permission with name '" + newName + "' already exists");
        }
        
        permission.setName(newName);
        return permissionRepository.save(permission);
    }

    public PermissionRolesResponse getRolesForPermission(Long permissionId) {
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            throw new RuntimeException("Permission not found with ID: " + permissionId);
        }
        
        Permission permission = permissionOpt.get();
        List<Role> roles = permission.getRoles();
        
        return new PermissionRolesResponse(
            permissionId,
            permission.getName(),
            roles,
            roles.size()
        );
    }

    @Transactional
    public DeletePermissionResponse deletePermission(Long permissionId) {
        Optional<Permission> permissionOpt = permissionRepository
        .findById(permissionId);
        
        if (permissionOpt.isEmpty()) {
            throw new RuntimeException("Permission not found with ID: " + permissionId);
        }
        
        Permission permission = permissionOpt.get();
        
        // Get all roles that have this permission (for response info only)
        List<Role> affectedRoles = permission.getRoles();
        
        // Soft delete the permission only - don't modify roles
        System.out.println("Before setDeleted - deleted status: " + permission.getDeleted());
        permission.setDeleted();
        System.out.println("After setDeleted - deleted status: " + permission.getDeleted());
        
        if (permission.getVersion() == null) {
            permission.setVersion(0L);
        }
        
        Permission savedPermission = permissionRepository.save(permission);
        System.out.println("After save - deleted status: " + savedPermission.getDeleted());
        
        // Verify the deletion was saved
        if (savedPermission.getDeleted() == null || savedPermission.getDeleted() == 0) {
            throw new RuntimeException("Failed to soft delete permission. Deleted status: " + savedPermission.getDeleted());
        }
        
        return new DeletePermissionResponse(
            permissionId,
            permission.getName(),
            true,
            affectedRoles,
            "Permission deleted successfully. Affected "
             + affectedRoles.size() + " role(s)."
        );
    }


    @Transactional
    public void softDeletePermission(Long permissionId) {
        permissionRepository.softDeleteById(permissionId);
    }

}