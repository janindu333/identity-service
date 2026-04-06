package com.baber.identityservice.identityservice.repository;

import com.baber.identityservice.identityservice.entity.UserCredential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
public class UserCredentialRepositoryTest {

    @Autowired
    private UserCredentialRepository repository;
    private UserCredential testUser;

    @BeforeEach
    public void setUp() {
        // Create a test user for each test
        testUser = new UserCredential();
        testUser.setName("testUser");
        testUser.setEmail("test@example.com");
        // Set other properties as needed
        repository.save(testUser).block(); // Block to ensure the user is saved before proceeding
    }

    @AfterEach
    public void tearDown() {
        // Delete the test user after each test
        repository.delete(testUser).block(); // Block to ensure the user is deleted before proceeding
    }
    @Test
    public void testFindByName() {
        StepVerifier.create(repository.findByName("testUser"))
                .expectNextMatches(user -> user.getName().equals("testUser"))
                .verifyComplete();
    }
    @Test
    public void testFindByEmail() {
        StepVerifier.create(repository.findByEmail("test@example.com"))
                .expectNextMatches(user -> user.getEmail().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    public void testUsernameAlreadyExists() {
        // Define a username that already exists in the database
        String existingUsername = "testUser";

        // Call the method to check if the username already exists
        StepVerifier.create(repository.findByName(existingUsername))
                // Expect to receive a non-empty result, indicating that the username already exists
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void testEmailAlreadyExists() {
        // Define a username that already exists in the database
        String existingEmail = "test@example.com";

        // Call the method to check if the username already exists
        StepVerifier.create(repository.findByEmail(existingEmail))
                // Expect to receive a non-empty result, indicating that the username already exists
                .expectNextCount(1)
                .verifyComplete();
    }
}
