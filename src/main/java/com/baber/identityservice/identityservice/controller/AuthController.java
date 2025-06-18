package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.ForgotPasswordRequest;
import com.baber.identityservice.identityservice.dto.TokenResponse;
import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.service.AuthRequest;
import com.baber.identityservice.identityservice.service.AuthService;
import com.baber.identityservice.identityservice.config.ServiceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final ServiceLogger logger = new ServiceLogger(AuthController.class);
    @Autowired
    private AuthService service;
    @Autowired
    private AuthenticationManager authenticationManager;
    @PostMapping("/register")
    public BaseResponse<String> register(@RequestBody UserCredential user) {
        logger.apiRequest("Register API called for user: " + user.getName());
        try {
            return service.saveUser(user);
        } catch (Exception e) {
            logger.error("Registration failed due to server error: " + e.getMessage());
            return new BaseResponse<>(false, null, 0, "User registration failed, please try again later", null);
        }
    }
    @PostMapping("/login")
    public BaseResponse<TokenResponse> login(@RequestBody AuthRequest authRequest) {
        long startTime = System.currentTimeMillis();
        logger.apiRequest("Login API called for user: " + authRequest.getUsername());
        try {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

            if (authenticate.isAuthenticated()) {
                String accessToken = service.generateAccessToken(authRequest.getUsername());
                String refreshToken = service.generateRefreshToken(authRequest.getUsername());
                TokenResponse tokenResponse = new TokenResponse(accessToken, refreshToken);
                logger.info("API: /login, Method: POST, Status: 200, Response Time: "
                        + (System.currentTimeMillis() - startTime) + "ms");
                return new BaseResponse<>(true, null, 0, null, tokenResponse);
            } else {
                logger.error("API: /login, Method: POST, Status: 401, Response Time: "
                        + (System.currentTimeMillis() - startTime) + "ms");
                return new BaseResponse<>(false, null, 0, "Invalid access", null);
            }
        } catch (Exception e) {
            logger.error("API: /login, Method: POST, Status: 500, Response Time: "
                    + (System.currentTimeMillis() - startTime) + "ms");
            return new BaseResponse<>(false, null, 0, "User or password is not matched", null);
        }
    }

    @GetMapping("/getToken")
    public BaseResponse<String> getAccessTokenByRefreshToken(@RequestParam("refreshToken") String refreshToken) {
        logger.apiRequest("Refresh token request received");
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String accessToken = service.getAccessTokenByRefreshToken(refreshToken);
            if (accessToken != null) {
                logger.info("Access token generated successfully from refresh token");
                return new BaseResponse<>(true, "Success", 0, null, accessToken);
            } else {
                logger.warn("Invalid refresh token provided");
                return new BaseResponse<>(false, null, 0, "Refresh token is not valid", null);
            }
        } else {
            logger.warn("Refresh token missing in the request");
            return new BaseResponse<>(false, null, 0, "Refresh token is missing", null);
        }
    }

    @GetMapping("/validate")
    public BaseResponse<String> validateToken(@RequestParam("token") String token) {
        logger.apiRequest("Token validation request received");
        if (service.validateToken(token)) {
            logger.info("Token is valid");
            return new BaseResponse<>(true, "Token is valid", 0, null, null);
        } else {
            logger.warn("Token is not valid");
            return new BaseResponse<>(false, null, 0, "Token is not valid", null);
        }
    }

    @PostMapping("/reset-password")
    public BaseResponse<String> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        logger.apiRequest("Password reset request received for user ID: " + forgotPasswordRequest.getId());
        boolean success = service.resetPassword(forgotPasswordRequest.getId(), forgotPasswordRequest.getNewPassword());

        if (success) {
            logger.info("Password reset successful for user ID: " + forgotPasswordRequest.getId());
            return new BaseResponse<>(true, null, 0, "Password successfully reset", null);
        } else {
            logger.warn("User not found for ID: " + forgotPasswordRequest.getId());
            return new BaseResponse<>(false, null, 0, "User is not found", null);
        }
    }

    @PutMapping("/addLocation")
    public BaseResponse<String> updateLocation(@RequestBody AddLocationRequest addLocationRequest) {
        logger.apiRequest("Update location request received for user ID: " + addLocationRequest.getUserId());
        if(service.updateLocation(addLocationRequest)){
            logger.info("Location successfully updated for user ID: " + addLocationRequest.getUserId());
            return new BaseResponse<>(true, null, 0, "Location successfully updated", null);
        } else {
            logger.warn("User not found for ID: " + addLocationRequest.getUserId());
            return new BaseResponse<>(false, null, 0, "User is not found",  null);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public BaseResponse<String> adminEndpoint() {
        logger.apiRequest("Admin endpoint accessed");
        return new BaseResponse<>(true, "Admin access granted", 0, null, null);
    }
}
