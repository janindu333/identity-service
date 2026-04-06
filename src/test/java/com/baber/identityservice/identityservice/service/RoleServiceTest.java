package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RoleServiceTest {
    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private RoleService roleService;
    public RoleServiceTest() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void testGetAllRoles() {
        // Create sample roles
        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("ROLE_ADMIN");
        role1.setDescription("Administrator role");

        Role role2 = new Role();
        role2.setId(2L);
        role2.setName("ROLE_USER");
        role2.setDescription("User role");

        // Mock the behavior of roleRepository.findAll to return Flux containing sample roles
        when(roleRepository.findAll()).thenReturn(Flux.just(role1, role2));

        // Call the getAllRoles method of roleService
        Flux<Role> resultFlux = roleService.getAllRoles();

        // Verify that the Flux emitted by getAllRoles contains the expected roles
        StepVerifier.create(resultFlux)
                .expectNext(role1)
                .expectNext(role2)
                .verifyComplete();
    }

//    @Test
//    public void testAddRole() {
//        // Define test data
//        String roleName = "TEST_ROLE";
//        String roleDescription = "Test role description";
//
//        // Mock the behavior of roleRepository.findByName
//        when(roleRepository.findByName(roleName)).thenReturn(Mono.empty());
//
//        // Mock the behavior of roleRepository.save
//        Role savedRole = new Role();
//        savedRole.setName(roleName);
//        savedRole.setDescription(roleDescription);
//
//        // Call the addRole method of roleService
//        Mono<Object> resultMono = roleService.addRole(roleName, roleDescription);
//
//        // Verify that the Mono emitted by addRole completes successfully
//        StepVerifier.create(resultMono)
//                .expectComplete() // Expect the publisher to complete
//                .verify();
//    }

    @Test
    public void testAddRole_RoleAlreadyExists() {
        // Arrange
        String name = "admin";
        String description = "Administrator role";
        Role existingRole = new Role();
        existingRole.setName(name);

        when(roleRepository.findByName(name)).thenReturn(Mono.just(existingRole));

        // Act & Assert
        StepVerifier.create(roleService.addRole(name, description))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Role with name " + name + " already exists."))
                .verify();

        verify(roleRepository, times(1)).findByName(name);
        verify(roleRepository, never()).save(any(Role.class));
    }


    @Test
    public void testAddRole_RoleDoesNotExist() {
        // Arrange
        String name = "testRole";
        String description = "Test Role Description";
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);

        // Mock repository behavior
        when(roleRepository.findByName(name)).thenReturn(Mono.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(Mono.just(role));

        // Act & Assert
        StepVerifier.create(roleService.addRole(name, description))
                .expectNext(role)
                .verifyComplete();
    }


    @Test
    public void testGetRoleNameById() {
        // Define test data
        long roleId = 1L;
        String roleName = "TEST_ROLE";

        // Mock the behavior of roleRepository.findById
        Role role = new Role();
        role.setId(roleId);
        role.setName(roleName);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));

        // Call the getRoleNameById method of roleService
        Mono<String> roleNameMono = roleService.getRoleNameById(roleId);

        // Verify that the Mono emitted by getRoleNameById contains the expected role name
        StepVerifier.create(roleNameMono)
                .expectNext(roleName)
                .verifyComplete();
    }

}