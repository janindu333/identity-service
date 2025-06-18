package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.entity.Role;
import com.baber.identityservice.identityservice.repository.PermissionRepository;
import com.baber.identityservice.identityservice.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private PermissionService permissionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetDefaultPermissions_WhenRoleExists() {
        // given
        Role role = new Role();
        role.setId(1);
        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");
        role.setPermissions(List.of(permission));

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        // when
        List<Permission> permissions = permissionService.getDefaultPermissions(1L);

        // then
        assertNotNull(permissions);
        assertEquals(1, permissions.size());
        assertEquals("READ_PRIVILEGES", permissions.get(0).getName());
    }

    @Test
    public void testGetDefaultPermissions_WhenRoleDoesNotExist() {
        // given
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        List<Permission> permissions = permissionService.getDefaultPermissions(1L);

        // then
        assertNull(permissions);
    }

    @Test
    public void testGetPermissionsBySalonAndRole_WhenRoleExists() {
        // given
        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);
        role.setPermissions(List.of(permission));

        when(roleRepository.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findBySalonAndRole(1L, "Admin")).thenReturn(Set.of(permission));

        // when
        Set<Permission> permissions = permissionService.getPermissionsBySalonAndRole(1L, "Admin");

        // then
        assertNotNull(permissions);
        assertEquals(1, permissions.size());
        assertTrue(permissions.stream().anyMatch(p -> p.getName().equals("READ_PRIVILEGES")));
    }

    @Test
    public void testGetPermissionsBySalonAndRole_WhenRoleDoesNotExist() {
        // given
        when(roleRepository.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.empty());

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> {
            permissionService.getPermissionsBySalonAndRole(1L, "Admin");
        });

        // then
        assertEquals("role called Admin does not found for the given salon", exception.getMessage());
    }

    @Test
    public void testUpdatePermissionsBySalonAndRole() {
        // given
        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);

        when(roleRepository.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.of(role));

        // when
        List<Permission> updatedPermissions = permissionService.updatePermissionsBySalonAndRole(1L, "Admin", List.of(permission));

        // then
        assertNotNull(updatedPermissions);
        assertEquals(1, updatedPermissions.size());
        assertEquals("READ_PRIVILEGES", updatedPermissions.get(0).getName());
        verify(roleRepository, times(1)).save(role);
    }

    @Test
    public void testSavePermission() {
        // given
        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");

        when(permissionRepository.save(permission)).thenReturn(permission);

        // when
        Permission savedPermission = permissionService.save(permission);

        // then
        assertNotNull(savedPermission);
        assertEquals("READ_PRIVILEGES", savedPermission.getName());
        verify(permissionRepository, times(1)).save(permission);
    }

    @Test
    public void testFindByName() {
        // given
        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");

        when(permissionRepository.findByName("READ_PRIVILEGES")).thenReturn(Optional.of(permission));

        // when
        Optional<Permission> found = permissionService.findByName("READ_PRIVILEGES");

        // then
        assertTrue(found.isPresent());
        assertEquals("READ_PRIVILEGES", found.get().getName());
    }

    @Test
    public void testAddPermissionToRole() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);

        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");

        when(roleRepository.findByNameAndSalonId("Admin", 1L)).thenReturn(Optional.of(role));

        // when
        List<Permission> permissions = permissionService.addPermissionToRole(1L, "Admin", permission);

        // then
        assertNotNull(permissions);
        assertTrue(permissions.contains(permission));
        verify(roleRepository, times(1)).save(role);
    }


    @Test
    public void testRemovePermissionFromRole_WhenPermissionNotExist() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);

        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("READ_PRIVILEGES");

        role.setPermissions(new ArrayList<>());

        when(roleRepository.findByIdAndSalonId(1L, 1L)).thenReturn(Optional.of(role));

        // when
        List<Permission> permissions = permissionService.removePermissionFromRole(1L, 1L, 1L);

        // then
        assertNull(permissions);
        verify(roleRepository, never()).save(role);
    }
}
