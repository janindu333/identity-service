package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

//    @Test
//    public void testSaveUser_Success() {
//        // given
//        UserCredential userCredential = new UserCredential();
//        userCredential.setName("newUser");
//        userCredential.setEmail("new@example.com");
//        userCredential.setPassword("password");
//
//        when(userCredentialRepository.findByName("newUser")).thenReturn(Optional.empty());
//        when(userCredentialRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
//        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
//
//        // when
//        BaseResponse<String> response = authService.saveUser(userCredential);
//
//        // then
//        assertTrue(response.isSuccess());
//        assertEquals("user added", response.getMessage());
//        verify(userCredentialRepository, times(1)).save(userCredential);
//    }

    @Test
    public void testGenerateAccessToken() {
        // given
        UserCredential userCredential = new UserCredential();
        userCredential.setName("user");
        userCredential.setRole("USER");

        when(userCredentialRepository.findByName("user")).thenReturn(Optional.of(userCredential));
        when(jwtService.generateToken("user", "USER", 30 * 60 * 1000)).thenReturn("accessToken");

        // when
        String token = authService.generateAccessToken("user");

        // then
        assertEquals("accessToken", token);
    }

    @Test
    public void testGenerateRefreshToken() {
        // given
        UserCredential userCredential = new UserCredential();
        userCredential.setName("user");
        userCredential.setRole("USER");

        when(userCredentialRepository.findByName("user")).thenReturn(Optional.of(userCredential));
        when(jwtService.generateToken("user", "USER", 7 * 24 * 60 * 60 * 1000)).thenReturn("refreshToken");

        // when
        String token = authService.generateRefreshToken("user");

        // then
        assertEquals("refreshToken", token);
    }

    @Test
    public void testGetAccessTokenByRefreshToken_ValidToken() {
        // given
        String refreshToken = "validRefreshToken";
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn("user");

        UserCredential userCredential = new UserCredential();
        userCredential.setName("user");
        userCredential.setRole("USER");

        when(userCredentialRepository.findByName("user")).thenReturn(Optional.of(userCredential));
        when(jwtService.generateToken("user", "USER", 30 * 60 * 1000)).thenReturn("newAccessToken");

        // when
        String token = authService.getAccessTokenByRefreshToken(refreshToken);

        // then
        assertEquals("newAccessToken", token);
    }

    @Test
    public void testGetAccessTokenByRefreshToken_InvalidToken() {
        // given
        String refreshToken = "invalidRefreshToken";
        when(jwtService.validateToken(refreshToken)).thenReturn(false);

        // when
        String token = authService.getAccessTokenByRefreshToken(refreshToken);

        // then
        assertNull(token);
    }

    @Test
    public void testValidateToken() {
        // given
        String token = "validToken";
        when(jwtService.validateToken(token)).thenReturn(true);

        // when
        boolean isValid = authService.validateToken(token);

        // then
        assertTrue(isValid);
    }

    @Test
    public void testFindById() {
        // given
        UserCredential userCredential = new UserCredential();
        userCredential.setId(1);
        userCredential.setName("user");

        when(userCredentialRepository.findById(1)).thenReturn(Optional.of(userCredential));

        // when
        UserCredential foundUser = authService.findById(1);

        // then
        assertNotNull(foundUser);
        assertEquals("user", foundUser.getName());
    }

    @Test
    public void testResetPassword() {
        // given
        UserCredential userCredential = new UserCredential();
        userCredential.setId(1);
        userCredential.setPassword("oldPassword");

        when(userCredentialRepository.findById(1)).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // when
        boolean isReset = authService.resetPassword(1, "newPassword");

        // then
        assertTrue(isReset);
        assertEquals("encodedNewPassword", userCredential.getPassword());
        verify(userCredentialRepository, times(1)).save(userCredential);
    }

}
