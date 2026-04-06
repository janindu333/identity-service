package com.baber.identityservice.identityservice.controller;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.ForgotPasswordRequest;
import com.baber.identityservice.identityservice.dto.TokenResponse;
import com.baber.identityservice.identityservice.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.service.AuthRequest;
import com.baber.identityservice.identityservice.service.AuthService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ReactiveAuthenticationManager reactiveAuthenticationManager;
    @PostMapping("/register")
    public Mono<BaseResponse<Object>> register(@RequestBody UserCredential user) {
        return authService.saveUser(user)
                .flatMap(response -> Mono.just(new BaseResponse<>(response.isSuccess(), response.getMessage(),
                        response.getErrorCode(), response.getErrorMessage(), response.getData())));
    }
    @PostMapping("/login")
    public Mono<BaseResponse<TokenResponse>> login(@RequestBody AuthRequest authRequest) {
        return authService.getUserCredentialByUsername(authRequest.getUsername())
                .flatMap(userCredential -> {
                    return reactiveAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken
                                    (authRequest.getUsername(), authRequest.getPassword()))
                            .flatMap(authentication -> {
                                String accessToken = authService.generateAccessToken(authRequest.getUsername());
                                String refreshToken = authService.generateRefreshToken(authRequest.getUsername());

                                // Get role ID from user credentials
                                Long roleId = userCredential.getData().getRoleId();

                                // Get role name by role ID
                                return roleService.getRoleNameById(roleId)
                                        .map(roleName -> {
                                            // Construct token response with role name
                                            TokenResponse tokenResponse = new TokenResponse(accessToken, refreshToken,
                                                    roleName);
                                            return new BaseResponse<>(true, "Success", 0,
                                                    null, tokenResponse);
                                        })
                                        .defaultIfEmpty(new BaseResponse<>(false, null, 404,
                                                "Role not found", null));
                            })
                            .onErrorResume(UsernameNotFoundException.class, error -> {
                                return Mono.just(new BaseResponse<>(false, null, 403,
                                        "User name not found", null));
                            })
                            .onErrorResume(BadCredentialsException.class, error -> {
                                return Mono.just(new BaseResponse<>(false, null, 401,
                                        "Bad credentials", null));
                            })
                            .onErrorResume(DisabledException.class, error -> {
                                return Mono.just(new BaseResponse<>(false, null, 403,
                                        "Account is disabled", null));
                            })
                            .onErrorResume(LockedException.class, error -> {
                                return Mono.just(new BaseResponse<>(false, null, 403,
                                        "Account is locked", null));
                            })
                            .onErrorResume(AccountExpiredException.class, error -> {
                                return Mono.just(new BaseResponse<>(false, null, 403,
                                        "Account is expired", null));
                            })
                            .onErrorResume(Exception.class, error -> {
                                return Mono.just(new BaseResponse<>(false, null, 500,
                                        "Internal server error", null));
                            });

                });
    }
    @GetMapping("/getToken")
    public BaseResponse<String> getAccessTokenByRefreshToken(@RequestParam("refreshToken") String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String accessToken = authService.getAccessTokenByRefreshToken(refreshToken);
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
        if (authService.validateToken(token)) {
            return new BaseResponse<>(true, "Token is valid", 0, null, null);
        } else {
            return new BaseResponse<>(false, null, 0, "Token is not valid", null);
        }
    }
    @PostMapping("/reset-password")
    public Mono<BaseResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        return authService.resetPassword(forgotPasswordRequest.getId(), forgotPasswordRequest.getNewPassword())
                .map(resetSuccess -> {
                    if (resetSuccess) {
                        return new BaseResponse<>(true, null, 0, "Password successfully reset", null);
                    } else {
                        return new BaseResponse<>(false, null, 0, "User not found", null);
                    }
                });
    }
    @PutMapping("/addLocation")
    public Mono<BaseResponse<String>> updateLocation(@RequestBody AddLocationRequest addLocationRequest) {
        return authService.updateLocation(addLocationRequest)
                .map(updated -> {
                    if (updated) {
                        return new BaseResponse<>(true, null, 0, "Location successfully updated", null);
                    } else {
                        return new BaseResponse<>(false, null, 0, "User not found", null);
                    }
                });
    }
}