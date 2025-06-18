package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllRoles() {
        // given
        Role role1 = new Role();
        role1.setId(1);
        role1.setName("Admin");

        Role role2 = new Role();
        role2.setId(2);
        role2.setName("User");

        when(roleRepository.findAll()).thenReturn(List.of(role1, role2));

        // when
        List<Role> roles = roleService.getAllRoles();

        // then
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertEquals("Admin", roles.get(0).getName());
        assertEquals("User", roles.get(1).getName());
    }

    @Test
    public void testGetRoleById() {
        // given
        Role role = new Role();
        role.setId(1);
        role.setName("Admin");

        when(roleRepository.findById(1)).thenReturn(Optional.of(role));

        // when
        Optional<Role> foundRole = roleService.getRoleById(1);

        // then
        assertTrue(foundRole.isPresent());
        assertEquals("Admin", foundRole.get().getName());
    }

    @Test
    public void testCreateRole_WhenRoleDoesNotExist() {
        // given
        Role role = new Role();
        role.setName("Admin");

        when(roleRepository.findByName("Admin")).thenReturn(Optional.empty());
        when(roleRepository.save(role)).thenReturn(role);

        // when
        Optional<Role> createdRole = roleService.createRole(role);

        // then
        assertTrue(createdRole.isPresent());
        assertEquals("Admin", createdRole.get().getName());
        verify(roleRepository, times(1)).save(role);
    }

    @Test
    public void testCreateRole_WhenRoleExists() {
        // given
        Role role = new Role();
        role.setName("Admin");

        when(roleRepository.findByName("Admin")).thenReturn(Optional.of(role));

        // when
        Optional<Role> createdRole = roleService.createRole(role);

        // then
        assertFalse(createdRole.isPresent());
        verify(roleRepository, never()).save(role);
    }

    @Test
    public void testUpdateRole_WhenRoleExists() {
        // given
        Role role = new Role();
        role.setId(1);
        role.setName("Admin");

        Role roleDetails = new Role();
        roleDetails.setName("User");
        roleDetails.setDescription("Regular user");

        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);

        // when
        Optional<Role> updatedRole = roleService.updateRole(1, roleDetails);

        // then
        assertTrue(updatedRole.isPresent());
        assertEquals("User", updatedRole.get().getName());
        assertEquals("Regular user", updatedRole.get().getDescription());
        verify(roleRepository, times(1)).save(role);
    }

    @Test
    public void testUpdateRole_WhenRoleDoesNotExist() {
        // given
        Role roleDetails = new Role();
        roleDetails.setName("User");
        roleDetails.setDescription("Regular user");

        when(roleRepository.findById(1)).thenReturn(Optional.empty());

        // when
        Optional<Role> updatedRole = roleService.updateRole(1, roleDetails);

        // then
        assertFalse(updatedRole.isPresent());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    public void testDeleteRole_WhenRoleExists() {
        // given
        Role role = new Role();
        role.setId(1);
        role.setName("Admin");

        when(roleRepository.findById(1)).thenReturn(Optional.of(role));

        // when
        boolean isDeleted = roleService.deleteRole(1);

        // then
        assertTrue(isDeleted);
        verify(roleRepository, times(1)).delete(role);
    }

    @Test
    public void testDeleteRole_WhenRoleDoesNotExist() {
        // given
        when(roleRepository.findById(1)).thenReturn(Optional.empty());

        // when
        boolean isDeleted = roleService.deleteRole(1);

        // then
        assertFalse(isDeleted);
        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    public void testFindByNameAndSalonId() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);

        when(roleRepository.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.of(role));

        // when
        Optional<Role> foundRole = roleService.findByNameAndSalonId("Admin", 1L);

        // then
        assertTrue(foundRole.isPresent());
        assertEquals("Admin", foundRole.get().getName());
    }
}
