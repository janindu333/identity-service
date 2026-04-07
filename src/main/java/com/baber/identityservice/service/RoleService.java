package com.baber.identityservice.service;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baber.identityservice.dto.AddRoleRequest;
import com.baber.identityservice.dto.UpdateRoleRequest;
import com.baber.identityservice.entity.Role;
import com.baber.identityservice.entity.Permission;
import com.baber.identityservice.repository.RoleRepository;
import com.baber.identityservice.repository.PermissionRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PermissionRepository permissionRepository;

        public List<Role> getAllRoles() {
                return roleRepository.findAll();
        }

        public Optional<Role> getRoleById(int id) {
                return roleRepository.findById(id);
        }

        public Optional<Role> createRole(AddRoleRequest roleRequest) {
                // Fetch all roles and check for name match ignoring case
                List<Role> allRoles = roleRepository.findAll();
                boolean exists = allRoles.stream()
                    .anyMatch(r -> r.getName() != null && r.getName().equalsIgnoreCase(roleRequest.getName()));
                if (exists) {
                        return Optional.empty(); // Indicate that the role already exists (case-insensitive)
                }
                // Map AddRoleRequest to Role entity
                Role role = new Role();
                role.setName(roleRequest.getName());
                role.setDescription(roleRequest.getDescription());
                // Add more fields if needed
                Role createdRole = roleRepository.save(role);
                return Optional.of(createdRole);
        }

        public Optional<Role> updateRole(int id, UpdateRoleRequest roleDetails) {
                return roleRepository.findById(id).map(role -> {
                        role.setName(roleDetails.getName());
                        role.setDescription(roleDetails.getDescription());
                        return roleRepository.save(role);
                });
        }

        public boolean deleteRole(int id) {
                return roleRepository.findById(id).map(role -> {
                        roleRepository.delete(role);
                        return true;
                }).orElse(false);
        }

 

        public Optional<Role> assignPermissionsToRole(int roleId, List<Long> permissionIds) {
                // Find the role
                Optional<Role> roleOptional = roleRepository.findById(roleId);
                if (roleOptional.isEmpty()) {
                        return Optional.empty();
                }

                Role role = roleOptional.get();

                // Find all permissions by their IDs
                List<Permission> permissions = permissionRepository.findAllById(permissionIds);
                
                // Check if all requested permissions were found
                if (permissions.size() != permissionIds.size()) {
                        // Some permissions were not found
                        List<Long> foundPermissionIds = permissions.stream()
                                .map(Permission::getId)
                                .collect(Collectors.toList());
                        List<Long> missingPermissionIds = permissionIds.stream()
                                .filter(id -> !foundPermissionIds.contains(id))
                                .collect(Collectors.toList());
                        
                        // You might want to throw an exception or handle this differently
                        System.out.println("Warning: Some permissions not found: " + missingPermissionIds);
                }

                // Assign permissions to role
                role.setPermissions(permissions);
                Role savedRole = roleRepository.save(role);
                
                return Optional.of(savedRole);
        }

        public Optional<Role> removePermissionFromRole(int roleId, Long permissionId) {
                // Find the role
                Optional<Role> roleOptional = roleRepository.findById(roleId);
                if (roleOptional.isEmpty()) {
                        return Optional.empty();
                }

                Role role = roleOptional.get();

                // Remove the permission from the role's permissions list
                boolean removed = role.getPermissions().removeIf(permission -> 
                        permission.getId().equals(permissionId));

                if (!removed) {
                        // Permission was not found in the role
                        return Optional.empty();
                }

                // Save the updated role
                Role savedRole = roleRepository.save(role);
                
                return Optional.of(savedRole);
        }
}

