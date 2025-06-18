package com.baber.identityservice.identityservice.repository;

import com.baber.identityservice.identityservice.entity.Permission;
import com.baber.identityservice.identityservice.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
@Transactional
public class PermissionRepositoryTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void whenFindByName_thenReturnPermission() {
        // given
        Permission permission = new Permission();
        permission.setName("READ_PRIVILEGES");
        permissionRepository.save(permission);

        // when
        Optional<Permission> found = permissionRepository.findByName("READ_PRIVILEGES");

        // then
        assertTrue(found.isPresent());
        assertEquals("READ_PRIVILEGES", found.get().getName());
    }


}
