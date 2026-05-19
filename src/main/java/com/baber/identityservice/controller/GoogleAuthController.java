package com.baber.identityservice.controller;

import com.baber.identityservice.config.ServiceLogger;
import com.baber.identityservice.dto.BaseResponse;
import com.baber.identityservice.dto.GoogleAuthorizeUrlResponse;
import com.baber.identityservice.dto.GoogleCallbackRequest;
import com.baber.identityservice.dto.GoogleCallbackResponse;
import com.baber.identityservice.dto.GoogleSignInRequest;
import com.baber.identityservice.dto.TokenResponse;
import com.baber.identityservice.service.AuthService;
import com.baber.identityservice.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/google")
@Tag(name = "Google Sign-In", description = "Google OAuth via Keycloak identity brokering")
public class GoogleAuthController {

    private static final ServiceLogger logger = new ServiceLogger(GoogleAuthController.class);
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String REMEMBER_ME_COOKIE = "rememberMe";

    private final GoogleAuthService googleAuthService;

    public GoogleAuthController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/authorize")
    @Operation(summary = "Get Keycloak Google login URL",
            description = "Redirect the browser to authorizationUrl. Configure Google as an IdP in Keycloak (alias: google).")
    public BaseResponse<GoogleAuthorizeUrlResponse> authorize(
            @RequestParam("redirectUri") String redirectUri,
            @RequestParam(value = "role", required = false) Long role,
            @RequestParam(value = "prompt", required = false) String prompt) {
        try {
            GoogleAuthorizeUrlResponse response = googleAuthService.buildAuthorizeUrl(redirectUri, role, prompt);
            return new BaseResponse<>(true, "Success", 0, null, response);
        } catch (GoogleAuthService.GoogleAuthDisabledException e) {
            return new BaseResponse<>(false, null, 503, e.getMessage(), null);
        } catch (GoogleAuthService.GoogleAuthException e) {
            return new BaseResponse<>(false, null, 400, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Google authorize failed: " + e.getMessage());
            return new BaseResponse<>(false, null, 500, "Failed to build Google authorization URL", null);
        }
    }

    @PostMapping("/callback")
    @Operation(summary = "Complete Google sign-in (authorization code from Keycloak)",
            description = "New users without role receive errorCode 4101 and must call again with role 3 or 7 and registrationToken.")
    public BaseResponse<GoogleCallbackResponse> callback(
            @RequestBody GoogleCallbackRequest request,
            HttpServletResponse response) {
        try {
            GoogleAuthService.GoogleCallbackOutcome outcome = googleAuthService.handleCallback(
                    request.getCode(),
                    request.getRedirectUri(),
                    request.getState(),
                    request.getRole(),
                    request.getRegistrationToken(),
                    false
            );
            if (outcome.isRoleSelection()) {
                return new BaseResponse<>(
                        false,
                        "Role selection required",
                        GoogleAuthService.REQUIRES_ROLE_SELECTION_ERROR_CODE,
                        null,
                        outcome.roleSelectionResponse());
            }
            setRefreshTokenCookie(response, outcome.loginResult().refreshToken(), false);
            setRememberMePreferenceCookie(response, false);
            return new BaseResponse<>(
                    true,
                    "Success",
                    0,
                    null,
                    GoogleCallbackResponse.fromLogin(
                            outcome.loginResult().tokenResponse(),
                            outcome.existingAccount()));
        } catch (GoogleAuthService.GoogleAuthDisabledException e) {
            return new BaseResponse<>(false, null, 503, e.getMessage(), null);
        } catch (GoogleAuthService.GoogleAuthException e) {
            return new BaseResponse<>(false, null, 400, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Google callback failed: " + e.getMessage());
            return new BaseResponse<>(false, null, 500, "Google sign-in failed", null);
        }
    }

    @PostMapping
    @Operation(summary = "Sign in with Google ID token (SPA / mobile)")
    public BaseResponse<TokenResponse> signInWithIdToken(
            @RequestBody GoogleSignInRequest request,
            HttpServletResponse response) {
        boolean rememberMe = request.getRememberMe() != null && request.getRememberMe();
        try {
            AuthService.KeycloakLoginResult result = googleAuthService.signInWithIdToken(
                    request.getIdToken(),
                    request.getRole(),
                    rememberMe
            );
            setRefreshTokenCookie(response, result.refreshToken(), rememberMe);
            setRememberMePreferenceCookie(response, rememberMe);
            return new BaseResponse<>(true, "Success", 0, null, result.tokenResponse());
        } catch (GoogleAuthService.GoogleAuthDisabledException e) {
            return new BaseResponse<>(false, null, 503, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return new BaseResponse<>(false, null, 400, e.getMessage(), null);
        } catch (GoogleAuthService.GoogleAuthException e) {
            return new BaseResponse<>(false, null, 400, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Google ID token sign-in failed: " + e.getMessage());
            return new BaseResponse<>(false, null, 500, "Google sign-in failed", null);
        }
    }

    private static int refreshCookieMaxAgeSeconds(boolean rememberMe) {
        return rememberMe ? 30 * 24 * 60 * 60 : 7 * 24 * 60 * 60;
    }

    private void setRememberMePreferenceCookie(HttpServletResponse response, boolean rememberMe) {
        ResponseCookie cookie = ResponseCookie.from(REMEMBER_ME_COOKIE, rememberMe ? "true" : "false")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshCookieMaxAgeSeconds(rememberMe))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, boolean rememberMe) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshCookieMaxAgeSeconds(rememberMe))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
