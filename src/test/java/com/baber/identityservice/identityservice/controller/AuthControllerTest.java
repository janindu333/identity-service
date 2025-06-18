package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.dto.ForgotPasswordRequest;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.service.AuthRequest;
import com.baber.identityservice.identityservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserCredential user;
    private AuthRequest authRequest;

    @BeforeEach
    public void setUp() {
        user = new UserCredential();
        user.setId(1);
        user.setName("testUser");
        user.setEmail("testUser@example.com");
        user.setPassword("password");

        authRequest = new AuthRequest();
        authRequest.setUsername("testUser");
        authRequest.setPassword("password");
    }

    @Test
    public void testRegister() throws Exception {
        BaseResponse<String> response = new BaseResponse<>(true, "user added", 0, null, null);
        when(authService.saveUser(any(UserCredential.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("user added"));
    }

    @Test
    public void testLogin() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authService.generateAccessToken(any())).thenReturn("access-token");
        when(authService.generateRefreshToken(any())).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }




    @Test
    public void testGetAccessTokenByRefreshToken() throws Exception {
        when(authService.getAccessTokenByRefreshToken(any())).thenReturn("new-access-token");

        mockMvc.perform(get("/auth/getToken")
                        .param("refreshToken", "valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("new-access-token"));
    }

    @Test
    public void testGetAccessTokenByInvalidRefreshToken() throws Exception {
        when(authService.getAccessTokenByRefreshToken(any())).thenReturn(null);

        mockMvc.perform(get("/auth/getToken")
                        .param("refreshToken", "invalid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false)) ;
    }

    @Test
    public void testValidateToken() throws Exception {
        when(authService.validateToken(any())).thenReturn(true);

        mockMvc.perform(get("/auth/validate")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    public void testResetPassword() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setId(1);
        request.setNewPassword("new-password");

        when(authService.resetPassword(eq(1), anyString())).thenReturn(true);

        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)) ;
    }
}
