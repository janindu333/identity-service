package com.baber.identityservice.identityservice.repository;
import com.baber.identityservice.identityservice.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
public class RoleRepositoryTest {
    @Autowired
    private RoleRepository roleRepository;
    private Role testRole;
    @BeforeEach
    public void setUp() {
        // Create a test role for each test
        testRole = new Role();
        testRole.setName("ROLE_TEST");
        testRole.setDescription("Test role");
        // Set other properties as needed
        roleRepository.save(testRole).block(); // Block to ensure the role is saved before proceeding
    }
    @AfterEach
    public void tearDown() {
        // Delete the test role after each test
        roleRepository.delete(testRole).block(); // Block to ensure the role is deleted before proceeding
    }

    @Test
    public void testFindByName() {
        StepVerifier.create(roleRepository.findByName("ROLE_TEST"))
                .expectNextMatches(role -> role.getName().equals("ROLE_TEST"))
                .verifyComplete();
    }

    @Test
    public void testRoleAlreadyExists() {
        // Define a role name that already exists in the database
        String existingRoleName = "ROLE_TEST";

        // Call the method to check if the role already exists
        StepVerifier.create(roleRepository.findByName(existingRoleName))
                // Expect to receive a non-empty result, indicating that the role already exists
                .expectNextMatches(role -> role.getName().equals(existingRoleName))
                .verifyComplete();
    }

}

// Add more test methods as needed to cover
