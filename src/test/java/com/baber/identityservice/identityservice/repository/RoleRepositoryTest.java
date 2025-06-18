package com.baber.identityservice.identityservice.repository;

import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test") // Use the test profile
@Transactional // Ensure each test is transactional and rolls back after execution
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    public void whenFindById_thenReturnRole() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);
        roleRepository.save(role);

        // when
        Optional<Role> found = roleRepository.findById(role.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getName());
    }

    @Test
    public void whenFindByNameAndSalonId_thenReturnRole() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);
        roleRepository.save(role);

        // when
        Optional<Role> found = roleRepository.findByNameAndSalonId("Admin", 1L);

        // then
        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getName());
    }

    @Test
    public void whenFindByIdAndSalonId_thenReturnRole() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);
        roleRepository.save(role);

        // when
        Optional<Role> found = roleRepository.findByIdAndSalonId((long) role.getId(), 1L);

        // then
        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getName());
    }

    @Test
    public void whenFindByName_thenReturnRole() {
        // given
        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);
        roleRepository.save(role);

        // when
        Optional<Role> found = roleRepository.findByName("Admin");

        // then
        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getName());
    }

    @Test
    public void whenFindBySalonAndRole_thenReturnPermissions() {
        // given
        Permission permission1 = new Permission();
        permission1.setName("READ_PRIVILEGES");
        permissionRepository.save(permission1);

        Permission permission2 = new Permission();
        permission2.setName("WRITE_PRIVILEGES");
        permissionRepository.save(permission2);

        // Ensure the permissions are managed entities
        permission1 = permissionRepository.findById(permission1.getId()).orElseThrow();
        permission2 = permissionRepository.findById(permission2.getId()).orElseThrow();

        Role role = new Role();
        role.setName("Admin");
        role.setSalonId(1L);
        role.setPermissions(List.of(permission1, permission2)); // Use a HashSet to avoid errors
        roleRepository.save(role);

        // when
        Set<Permission> permissions = permissionRepository.findBySalonAndRole(1L, "Admin");

        // then
        assertNotNull(permissions);
        assertEquals(2, permissions.size());
        assertTrue(permissions.stream().anyMatch(permission -> permission.getName().equals("READ_PRIVILEGES")));
        assertTrue(permissions.stream().anyMatch(permission -> permission.getName().equals("WRITE_PRIVILEGES")));
    }
}
