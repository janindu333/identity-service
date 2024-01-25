package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.ForgotPasswordRequest;
import com.baber.identityservice.identityservice.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.service.AuthRequest;
import com.baber.identityservice.identityservice.service.AuthService;
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService service;
    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public BaseResponse<String> addNewUser(@RequestBody UserCredential user) {

        return service.saveUser(user);
    }
    @PostMapping("/login")
    public BaseResponse<TokenResponse> getToken(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            if (authenticate.isAuthenticated()) {
                String accessToken = service.generateAccessToken(authRequest.getUsername());
                String refreshToken = service.generateRefreshToken(authRequest.getUsername());
                TokenResponse tokenResponse = new TokenResponse(accessToken, refreshToken);
                return new BaseResponse<>(true, null, 0, null, tokenResponse);
            } else {
           return new BaseResponse<>(false, null, 0, "Invalied access", null);
            }
        } catch (Exception e) {
               return new BaseResponse<>(false, null, 0, "user or password is not matched", null);
        }
    }
    @GetMapping("/getToken")
    public BaseResponse<String> getAccessTokenByRefreshToken(@RequestParam("refreshToken") String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String accessToken = service.getAccessTokenByRefreshToken(refreshToken);
            if (accessToken != null) {
                return new BaseResponse<>(true, "success", 0, null, accessToken);
            } else {
                return new BaseResponse<>(false, null, 0, "refresh token is not valid", null);
            }
        } else {
            return new BaseResponse<>(false, null, 0, "refresh token is missing", null);
        }
    }
    @GetMapping("/validate")
    public BaseResponse<String> validateToken(@RequestParam("token") String token) {
        if (service.validateToken(token)) {
            return new BaseResponse<>(true, "Token is valid", 0, null, null);
        } else {
            return new BaseResponse<>(false, null, 0, "Token is not valid", null);
        }
    }
    @PostMapping("/reset-password")
    public BaseResponse<String> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {

        if(service.resetPassword(forgotPasswordRequest.getId(),forgotPasswordRequest.getNewPassword())){
            return new BaseResponse<>(true, null, 0, "password successfully reset", null);

        } else {
            return new BaseResponse<>(false, null, 0, "User is not found",  null);
        }
    }

    @PutMapping("/addLocation")
    public BaseResponse<String> updateLocation(@RequestBody AddLocationRequest addLocationRequest) {

        if(service.updateLocation(addLocationRequest)){
            return new BaseResponse<>(true, null, 0, "location successfully updated", null);
        } else {
            return new BaseResponse<>(false, null, 0, "User is not found",  null);
        }
    }
}