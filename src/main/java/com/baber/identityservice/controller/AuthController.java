package com.baber.identityservice.controller;

import com.baber.identityservice.config.ClientIpResolver;
import com.baber.identityservice.config.RateLimitProperties;
import com.baber.identityservice.config.ServiceLogger;
import com.baber.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.dto.BaseResponse;
import com.baber.identityservice.dto.ForgotPasswordRequest;
import com.baber.identityservice.dto.ResetPasswordRequest;
import com.baber.identityservice.dto.TokenResponse;
import com.baber.identityservice.dto.RegistrationResult;
import com.baber.identityservice.dto.UserRegistrationRequest;
import com.baber.identityservice.dto.ValidateTokenResponse;
import com.baber.identityservice.dto.OnboardingStatusResponse;
import com.baber.identityservice.service.AuthRequest;
import com.baber.identityservice.service.AuthService;
import com.baber.identityservice.service.RateLimiterService;
import com.baber.identityservice.service.RateLimiterService.RateLimitResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/auth") 
public class AuthController {
    private static final ServiceLogger logger = new ServiceLogger(AuthController.class);
    @Autowired
    private AuthService service;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RateLimiterService rateLimiterService;
    @Autowired
    private RateLimitProperties rateLimitProperties;
    @Autowired
    private ClientIpResolver clientIpResolver;


    @PostMapping("/signup")
    public BaseResponse<String> register(@RequestBody UserRegistrationRequest user) {
        logger.apiRequest("Register API called for user: " + user.getFirstName() + " " + user.getLastName());
        try {
            RegistrationResult result = service.saveUser(user);

            if (result.isSuccess()) {
                return new BaseResponse<>(
                    true,
                    result.getMessage(),
                    0,
                    null,
                    null
                );
            } else {
                return new BaseResponse<>(
                    false,
                    null,
                    result.getErrorCode(),
                    result.getErrorMessage(),
                    null
                );
            }
        } catch (Exception e) {
            logger.error("Registration failed due to server error: " + e.getMessage());
            return new BaseResponse<>(false, null, 0, "User registration failed, please try again later", null);
        }
    }
    
    @PostMapping("/login")
    public BaseResponse<TokenResponse> login(@RequestBody AuthRequest authRequest,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        logger.apiRequest("Login API called for user: " + authRequest.getUsernameOrEmail() 
            + " (rememberMe: " + authRequest.getRememberMe() + ")");

        String clientIp = clientIpResolver.resolve(request);
        RateLimitResult ipLimit = rateLimiterService.checkLimit(
            "auth:login:ip:" + clientIp,
            rateLimitProperties.getLogin().getIp()
        );
        if (!ipLimit.isAllowed()) {
            logger.warn("Login blocked due to IP rate limit. IP: " + clientIp);
            return buildRateLimitedResponse(
                response,
                "Too many login attempts from this IP. Try again in " + ipLimit.getRetryAfterSeconds() + " seconds.",
                ipLimit.getRetryAfterSeconds()
            );
        }

        String usernameKey = authRequest.getUsernameOrEmail();
        if (usernameKey != null && !usernameKey.isBlank()) {
            RateLimitResult userLimit = rateLimiterService.checkLimit(
                "auth:login:user:" + usernameKey.toLowerCase(),
                rateLimitProperties.getLogin().getUsername()
            );
            if (!userLimit.isAllowed()) {
                logger.warn("Login blocked due to username rate limit. User: " + usernameKey);
                return buildRateLimitedResponse(
                    response,
                    "Too many login attempts for this account. Try again in " + userLimit.getRetryAfterSeconds() + " seconds.",
                    userLimit.getRetryAfterSeconds()
                );
            }
        }

        try {
            Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsernameOrEmail(), authRequest.getPassword()));

            if (authenticate.isAuthenticated()) {
                // Generate access token (always 2 hours)
                String accessToken = service.generateAccessTokenByUsernameOrEmail(authRequest.getUsernameOrEmail());
                
                // Generate refresh token with expiration based on rememberMe flag
                boolean rememberMe = authRequest.getRememberMe() != null && authRequest.getRememberMe();
                String refreshToken = service.generateRefreshTokenByUsernameOrEmail(
                    authRequest.getUsernameOrEmail(), rememberMe);
                
                // Set refresh token in HTTP-only cookie
                setRefreshTokenCookie(response, refreshToken, rememberMe);
                
                // Get user and onboarding status for owners
                TokenResponse tokenResponse = service.createLoginResponse(accessToken, authRequest.getUsernameOrEmail());
                
                logger.info("API: /login, Method: POST, Status: 200, Response Time: "
                        + (System.currentTimeMillis() - startTime) + "ms, RememberMe: " + rememberMe);
                return new BaseResponse<>(true, null, 0, null, tokenResponse);
            } else {
                logger.error("API: /login, Method: POST, Status: 401, Response Time: "
                        + (System.currentTimeMillis() - startTime) + "ms");
                return new BaseResponse<>(false, null, 0, "Invalid password", null);
            }
        } catch (Exception e) {
            logger.error("API: /login, Method: POST, Status: 500, Response Time: "
                    + (System.currentTimeMillis() - startTime) + "ms, Error: " + e.getMessage());
            e.printStackTrace();
            return new BaseResponse<>(false, null, 0, "User or password is not matched", null);
        }
    }
    
    /**
     * Helper method to set refresh token as HTTP-only cookie
     * @param response - HTTP response
     * @param refreshToken - JWT refresh token
     * @param rememberMe - if true, cookie expires in 30 days; otherwise 7 days
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, boolean rememberMe) {
        int maxAge = rememberMe 
            ? 30 * 24 * 60 * 60  // 30 days in seconds
            : 7 * 24 * 60 * 60;   // 7 days in seconds
        
        // Create HTTP-only, Secure cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)           // Cannot be accessed by JavaScript (XSS protection)
            .secure(true)             // Only sent over HTTPS (set to false for localhost testing)
            .path("/")                // Available for all paths
            .maxAge(maxAge)           // Cookie expiration
            .sameSite("Strict")       // CSRF protection
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        logger.info("Refresh token cookie set with expiration: " + (rememberMe ? "30 days" : "7 days"));
    }

    /**
     * Legacy endpoint - uses query parameter for refresh token
     * For backward compatibility only. Use /refresh endpoint instead.
     */
    @GetMapping("/getToken")
    public BaseResponse<String> getAccessTokenByRefreshToken(@RequestParam("refreshToken") String refreshToken) {
        logger.apiRequest("Refresh token request received (legacy endpoint)");
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
    
    /**
     * NEW: Refresh access token using HTTP-only cookie
     * This is the recommended approach for security
     */
    @PostMapping("/refresh")
    public BaseResponse<TokenResponse> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        logger.apiRequest("Refresh token request received from cookie");

        String clientIp = clientIpResolver.resolve(request);
        RateLimitResult ipLimit = rateLimiterService.checkLimit(
            "auth:refresh:ip:" + clientIp,
            rateLimitProperties.getRefresh().getIp()
        );
        if (!ipLimit.isAllowed()) {
            logger.warn("Refresh blocked due to IP rate limit. IP: " + clientIp);
            return buildRateLimitedResponse(
                response,
                "Too many refresh attempts. Try again in " + ipLimit.getRetryAfterSeconds() + " seconds.",
                ipLimit.getRetryAfterSeconds()
            );
        }
        
        // Extract refresh token from HTTP-only cookie
        String refreshToken = getRefreshTokenFromCookie(request);
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            logger.warn("Refresh token cookie not found");
            return new BaseResponse<>(false, null, 0, "Refresh token is missing", null);
        }
        
        // Generate new access token
        String accessToken = service.getAccessTokenByRefreshToken(refreshToken);
        
        if (accessToken != null) {
            logger.info("Access token generated successfully from cookie refresh token");
            TokenResponse tokenResponse = new TokenResponse(accessToken, 7200L); // 2 hours
            return new BaseResponse<>(true, null, 0, null, tokenResponse);
        } else {
            logger.warn("Invalid refresh token from cookie");
            return new BaseResponse<>(false, null, 0, "Refresh token is not valid or expired", null);
        }
    }

    private BaseResponse<TokenResponse> buildRateLimitedResponse(HttpServletResponse response,
                                                                 String message,
                                                                 long retryAfterSeconds) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        if (retryAfterSeconds > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }
        return new BaseResponse<>(false, null, HttpStatus.TOO_MANY_REQUESTS.value(), message, null);
    }
    
    /**
     * Logout endpoint - clears the refresh token cookie
     */
    @PostMapping("/logout")
    public BaseResponse<String> logout(HttpServletResponse response) {
        logger.apiRequest("Logout request received");
        
        // Clear refresh token cookie by setting maxAge to 0
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)              // Expire immediately
            .sameSite("Strict")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        logger.info("User logged out successfully, refresh token cookie cleared");
        
        return new BaseResponse<>(true, "Logged out successfully", 0, null, null);
    }
    
    /**
     * Helper method to extract refresh token from cookies
     * @param request - HTTP request
     * @return refresh token string or null if not found
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
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

    /**
     * Legacy endpoint - resets password using userId.
     * @deprecated Use /reset-password/token endpoint instead for token-based password reset.
     */
    @PostMapping("/reset-password")
    public BaseResponse<String> forgotPassword(@RequestBody ForgotPasswordRequest 
        forgotPasswordRequest) {
        logger.apiRequest("Password reset request received for user ID: " + forgotPasswordRequest.getUserId());
        
        // Validate userId is provided
        if (forgotPasswordRequest.getUserId() == null) {
            logger.warn("Password reset attempted without userId. Please use /reset-password/token endpoint with token from email.");
            return new BaseResponse<>(false, null, 400, 
                "Invalid request. Please use the password reset token from your email. Use endpoint: /reset-password/token?token=YOUR_TOKEN&newPassword=YOUR_PASSWORD", null);
        }
        
        // Validate newPassword is provided
        if (forgotPasswordRequest.getNewPassword() == null || forgotPasswordRequest.getNewPassword().isEmpty()) {
            return new BaseResponse<>(false, null, 400, "New password is required", null);
        }
        
        boolean success = service.resetPassword(forgotPasswordRequest.getUserId(), forgotPasswordRequest.getNewPassword());

        if (success) {
            logger.info("Password reset successful for user ID: " + forgotPasswordRequest.getUserId());
            return new BaseResponse<>(true, "Password successfully reset", 0, null, null);
        } else {
            logger.warn("User not found for ID: " + forgotPasswordRequest.getUserId());
            return new BaseResponse<>(false, null, 404, "User is not found", null);
        }
    }

    /**
     * Request password reset by email (forgot password).
     * Returns error if email doesn't exist, success if email was sent.
     */
    @PostMapping("/forgot-password")
    public BaseResponse<String> requestPasswordReset(@RequestBody ForgotPasswordRequest request,
                                                     HttpServletRequest httpRequest) {
        logger.apiRequest("Password reset link requested for email: " + request.getEmail());

        String ipAddress = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        return service.requestPasswordReset(request.getEmail(), ipAddress, userAgent);
    }

    /**
     * Validate password reset token (for frontend when opening reset page).
     */
    @GetMapping("/validate-reset-token")
    public BaseResponse<ValidateTokenResponse> validateResetToken(@RequestParam("token") String token) {
        logger.apiRequest("Validate password reset token request received");
        ValidateTokenResponse result = service.validateResetToken(token);
        if (result.isValid()) {
            return new BaseResponse<>(true, "Token is valid", 0, null, result);
        } else {
            return new BaseResponse<>(false, null, 0, result.getMessage(), result);
        }
    }

    /**
     * Reset password using token + new password (token-based flow).
     * Supports both query parameters and request body for flexibility.
     */
    @PostMapping("/reset-password/token")
    public BaseResponse<String> resetPasswordByToken(
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestParam(value = "newPassword", required = false) String newPasswordParam,
            @RequestBody(required = false) ResetPasswordRequest requestBody) {
        logger.apiRequest("Password reset by token requested");
        
        // Get token from either query parameter or request body
        String token = tokenParam != null ? tokenParam : (requestBody != null ? requestBody.getToken() : null);
        String newPassword = newPasswordParam != null ? newPasswordParam : (requestBody != null ? requestBody.getNewPassword() : null);
        
        // Validate required parameters
        if (token == null || token.isEmpty()) {
            return new BaseResponse<>(false, null, 400, "Token is required", null);
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return new BaseResponse<>(false, null, 400, "New password is required", null);
        }
        
        return service.resetPasswordByToken(token, newPassword);
    }

    @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
    @PatchMapping("/location")
    public BaseResponse<String> updateLocation(@RequestBody AddLocationRequest addLocationRequest) {

        if(service.updateLocation(addLocationRequest)){
            return new BaseResponse<>(true, null, 0, "location successfully updated", null);
        } else {
            return new BaseResponse<>(false, null, 0, "User is not found",  null);
        }
    }
  
    @GetMapping("/admin")
    public BaseResponse<String> adminEndpoint() {
        logger.apiRequest("Admin endpoint accessed");
        return new BaseResponse<>(true, "Admin access granted", 0, null, null);
    }

    /**
     * Verify email address using token from email.
     * For salon owners, this returns an access token so they can immediately
     * continue onboarding (e.g., create their salon) after verification.
     */
    @PostMapping("/verify-email")
    public BaseResponse<TokenResponse> verifyEmail(@RequestParam("token") String token) {
        logger.apiRequest("Email verification request received");
        return service.verifyEmail(token);
    }

    /**
     * Resend email verification
     */
    @PostMapping("/resend-verification")
    public BaseResponse<String> resendVerificationEmail(@RequestBody ForgotPasswordRequest request) {
        logger.apiRequest("Resend verification email requested for: " + request.getEmail());
        return service.resendVerificationEmail(request.getEmail());
    }

    /**
     * Get onboarding status for the current user
     * Requires authentication - user ID extracted from JWT token
     */
    @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
    @GetMapping("/onboarding/status")
    public BaseResponse<OnboardingStatusResponse> getOnboardingStatus() {
        logger.apiRequest("Get onboarding status requested");
        // Note: User ID should be extracted from JWT token in a real implementation
        // For now, this endpoint structure is ready - needs user context extraction
        return new BaseResponse<>(false, null, 0, "Endpoint needs user context implementation", null);
    }
}
