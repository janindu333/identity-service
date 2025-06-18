package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.repository.RoleRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

        @Autowired
        private RoleRepository roleRepository;

        public List<Role> getAllRoles() {
                return roleRepository.findAll();
        }

        public Optional<Role> getRoleById(int id) {
                return roleRepository.findById(id);
        }

        public Optional<Role> createRole(Role role) {
                Optional<Role> existingRole = roleRepository.findByName(role.getName());
                if (existingRole.isPresent()) {
                        return Optional.empty(); // Indicate that the role already exists
                }
                Role createdRole = roleRepository.save(role);
                return Optional.of(createdRole);
        }

        public Optional<Role> updateRole(int id, Role roleDetails) {
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

        public Optional<Role> findByNameAndSalonId(String name, Long salonId) {
                return roleRepository.findByNameAndSalonId(name, salonId);
        }
}

