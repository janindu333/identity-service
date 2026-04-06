package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.UserCredentialRepository;
import com.baber.identityservice.identityservice.service.AuthService;
import com.baber.identityservice.identityservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSaveUser_SuccessfulSave() {
        // Arrange
        UserCredential newUser = new UserCredential();
        newUser.setName("newUser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");

        when(userCredentialRepository.findByName("newUser")).thenReturn(Mono.empty());
        when(userCredentialRepository.findByEmail("new@example.com")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(Mono.just(newUser));

        // Act & Assert
        StepVerifier.create(authService.saveUser(newUser))
                .expectNextMatches(response -> response.isSuccess() && response.getMessage().equals("user added"))
                .verifyComplete();
    }



    @Test
    public void testGenerateAccessToken() {
        // Arrange
        String username = "testUser";
        when(jwtService.generateToken(username, 30 * 60 * 1000)).thenReturn("accessToken");

        // Act
        String token = authService.generateAccessToken(username);

        // Assert
        assert token.equals("accessToken");
    }

    @Test
    public void testGenerateRefreshToken() {
        // Arrange
        String username = "testUser";
        when(jwtService.generateToken(username, 7 * 24 * 60 * 60 * 1000)).thenReturn("refreshToken");

        // Act
        String token = authService.generateRefreshToken(username);

        // Assert
        assert token.equals("refreshToken");
    }

    @Test
    public void testGetAccessTokenByRefreshToken() {
        // Arrange
        String refreshToken = "refreshToken";
        String username = "testUser";
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(jwtService.generateToken(username, 30 * 60 * 1000)).thenReturn("accessToken");

        // Act
        String token = authService.getAccessTokenByRefreshToken(refreshToken);

        // Assert
        assert token.equals("accessToken");
    }

    @Test
    public void testValidateToken() {
        // Arrange
        String token = "token";
        when(jwtService.validateToken(token)).thenReturn(true);

        // Act
        boolean isValid = authService.validateToken(token);

        // Assert
        assert isValid;
    }

    @Test
    public void testFindById() {
        // Arrange
        int userId = 1;
        UserCredential user = new UserCredential();
        user.setId(userId);
        when(userCredentialRepository.findById(userId)).thenReturn(Mono.just(user));

        // Act & Assert
        StepVerifier.create(authService.findById(userId))
                .expectNextMatches(foundUser -> foundUser.getId() == userId)
                .verifyComplete();
    }

    @Test
    public void testResetPassword() {
        // Arrange
        int userId = 1;
        String newPassword = "newPassword";
        UserCredential user = new UserCredential();
        user.setId(userId);
        user.setPassword("oldPassword");

        when(userCredentialRepository.findById(userId)).thenReturn(Mono.just(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userCredentialRepository.save(user)).thenReturn(Mono.just(user));

        // Act & Assert
        StepVerifier.create(authService.resetPassword(userId, newPassword))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testUpdateLocation() {
        // Arrange
        AddLocationRequest addLocationRequest = new AddLocationRequest();
        addLocationRequest.setUserId(1);
        addLocationRequest.setLatitude("37.7749");
        addLocationRequest.setLongitude("122.4194");

        UserCredential user = new UserCredential();
        user.setId(1);

        when(userCredentialRepository.findById(1)).thenReturn(Mono.just(user));
        when(userCredentialRepository.save(user)).thenReturn(Mono.just(user));

        // Act & Assert
        StepVerifier.create(authService.updateLocation(addLocationRequest))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testGetUserCredentialByUsername() {
        // Arrange
        String username = "testUser";
        UserCredential user = new UserCredential();
        user.setName(username);

        when(userCredentialRepository.findByName(username)).thenReturn(Mono.just(user));

        // Act & Assert
        StepVerifier.create(authService.getUserCredentialByUsername(username))
                .expectNextMatches(response -> response.isSuccess() && response.getMessage().equals("User found") && response.getData() != null)
                .verifyComplete();
    }
}

