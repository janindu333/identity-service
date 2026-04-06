package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Service
public class RoleService {
    private final RoleRepository roleRepository;
    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    public void saveRole(Role role) {
        roleRepository.save(role);
    }
    public Flux<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Mono<Object> addRole(String name, String description) {
        return roleRepository.findByName(name)
                .flatMap(existingRole ->
                        Mono.error(new IllegalArgumentException("Role with name " + name
                        + " already exists.")))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            // Create and save the new role
                            Role role = new Role();
                            role.setName(name);
                            role.setDescription(description);
                            return roleRepository.save(role);
                        })
                );
    }
    public Mono<String> getRoleNameById(Long id) {
        Mono<Role> roleMono = roleRepository.findById(id);
        return roleMono.map(role -> role != null ? role.getName() : null);
    }
}
