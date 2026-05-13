package com.baber.identityservice.service;

import com.baber.identityservice.dto.BaseResponse;
import com.baber.identityservice.dto.UserRegistrationRequest;
import com.baber.identityservice.dto.RegistrationResult;
import com.baber.identityservice.dto.ValidateTokenResponse;
import com.baber.identityservice.dto.TokenResponse;
import com.baber.identityservice.dto.LoginUserResponse;
import com.baber.identityservice.entity.UserCredential;
import com.baber.identityservice.entity.Role;
import com.baber.identityservice.entity.PasswordResetToken;
import com.baber.identityservice.entity.PasswordHistory;
import com.baber.identityservice.repository.UserCredentialRepository;
import com.baber.identityservice.repository.RoleRepository;
import com.baber.identityservice.repository.PasswordResetTokenRepository;
import com.baber.identityservice.repository.PasswordHistoryRepository;
import com.baber.identityservice.dto.OnboardingStatusResponse;
import com.baber.identityservice.config.ServiceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.baber.identityservice.dto.AddLocationRequest;

@Service
public class AuthService {
    private final ServiceLogger logger = new ServiceLogger(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserCredentialRepository userCredentialRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private SalonClient salonClient;

    @Autowired
    private KeycloakService keycloakService;

    @Value("${app.password-reset.token-expiration-minutes:60}")
    private int tokenExpirationMinutes;

    @Value("${app.password-reset.reset-url:http://localhost:3000/reset-password}")
    private String resetUrl;

    @Value("${app.password-history.check-count:5}")
    private int passwordHistoryCheckCount; // Number of recent passwords to check

    @Transactional
    public RegistrationResult saveUser(UserRegistrationRequest request) {
        logger.info("Saving user: " + request.getFirstName() + " " + request.getLastName());
        try {
            // Determine role to assign in Keycloak
            Role userRole;
            if (request.getRole() != null) {
                Optional<Role> requestedRole = roleRepository.findById(request.getRole().longValue());
                if (requestedRole.isPresent()) {
                    userRole = requestedRole.get();
                } else {
                    return new RegistrationResult(false, null, 400, "Invalid role ID provided");
                }
            } else {
                // Default to Client/Customer role (ID: 7 based on specification)
                Optional<Role> defaultRole = roleRepository.findByName("Customer");
                if (defaultRole.isPresent()) {
                    userRole = defaultRole.get();
                } else {
                    return new RegistrationResult(false, null, 500, "Default role not found in system");
                }
            }

            // Determine behavior based on role name from mirrored DB role.
            // Role IDs can change after data cleanup/sync, so avoid hardcoded numeric checks.
            String normalizedRoleName = userRole.getName() == null ? "" : userRole.getName().trim().toLowerCase();
            boolean isOwner = "owner".equals(normalizedRoleName);
            boolean emailVerified = !isOwner;

            // Duplicate email check in Keycloak
            KeycloakService.KeycloakUserProfile existingKeycloakUser = keycloakService.findUserByEmail(request.getEmail());
            if (existingKeycloakUser != null) {
                String requestedEmail = request.getEmail() == null ? "" : request.getEmail().trim();
                String keycloakEmail = existingKeycloakUser.email() == null ? "" : existingKeycloakUser.email().trim();
                if (requestedEmail.equalsIgnoreCase(keycloakEmail)) {
                    UserCredential existingLocal = ensureLocalUserProfile(existingKeycloakUser.id());
                    boolean isOwnerInKeycloak = existingKeycloakUser.realmRoles() != null
                            && existingKeycloakUser.realmRoles().stream().anyMatch(r -> "owner".equalsIgnoreCase(r));
                    if (!Boolean.TRUE.equals(existingKeycloakUser.emailVerified()) && isOwnerInKeycloak) {
                        sendEmailVerification(existingLocal);
                        return new RegistrationResult(
                                true,
                                "An account with this email already exists but is not verified. We have sent you a new verification email. Please check your inbox.",
                                0,
                                null
                        );
                    }
                    return new RegistrationResult(false, null, 409, "User email already exists");
                }
                logger.warn("Ignoring Keycloak user hit for non-matching email. requested=" + requestedEmail
                        + ", keycloak=" + keycloakEmail + ", userId=" + existingKeycloakUser.id());
            }

            String keycloakUserId;
            try {
                keycloakUserId = keycloakService.createUser(
                        request.getEmail(),
                        request.getFirstName(),
                        request.getLastName(),
                        request.getPhone(),
                        request.getPassword(),
                        emailVerified,
                        mapRoleNameForKeycloak(userRole.getName())
                );
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                    logger.warn("User email already exists in Keycloak: " + request.getEmail());
                    return new RegistrationResult(false, null, 409, "User email already exists");
                }
                String body = e.getResponseBodyAsString();
                String detail = body != null && !body.isBlank() ? body : e.getMessage();
                logger.error("Keycloak HTTP " + e.getStatusCode().value() + " for signup "
                        + request.getEmail() + ": " + detail);
                return new RegistrationResult(false, null, 502, "Failed to create user in identity provider");
            } catch (Exception keycloakError) {
                logger.error("Failed to create user in Keycloak: " + keycloakError.getMessage());
                return new RegistrationResult(false, null, 502, "Failed to create user in identity provider");
            }

            UserCredential userCredential = ensureLocalUserProfile(keycloakUserId);
            logger.success("User profile saved successfully for keycloakUserId: " + keycloakUserId + " with role: " + userRole.getName());
            
            if (isOwner) {
                // Generate email verification token and send email for owners only
                sendEmailVerification(userCredential);
                return new RegistrationResult(
                    true,
                    "User registered successfully. Please check your email to verify your account.",
                    0,
                    null
                );
            } else {
                // For clients, no email verification required
                return new RegistrationResult(
                    true,
                    "User registered successfully. You can sign in immediately.",
                    0,
                    null
                );
            }
        } catch (Exception e) {
            logger.error("Error saving user: " + request.getFirstName() + " " + request.getLastName() + ", Error: " + e.getMessage());
            return new RegistrationResult(false, null, 500, "User registration failed, please try again later");
        }
    }

    public String generateAccessToken(String username) {
        logger.info("Generating access token for user: " + username);
        UserCredential user = findUserByUsernameOrEmail(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return jwtService.generateAccessToken(username, "user", user.getId(), 120 * 60 * 1000L);
    }

    public String generateRefreshToken(String username) {
        logger.info("Generating refresh token for user: " + username);
        UserCredential user = findUserByUsernameOrEmail(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return jwtService.generateRefreshToken(username, "user");
    }

    public UserCredential findUserByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Finding user by username or email: " + usernameOrEmail);
        KeycloakService.KeycloakUserProfile keycloakUser = keycloakService.findUserByEmail(usernameOrEmail);
        if (keycloakUser != null) {
            return ensureLocalUserProfile(keycloakUser.id());
        }
        return null;
    }

    public UserCredential findUserByKeycloakUserId(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return null;
        }
        return userCredentialRepository.findByKeycloakUserId(keycloakUserId).orElse(null);
    }

    public String generateAccessTokenByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Generating access token for user: " + usernameOrEmail);
        UserCredential user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return jwtService.generateAccessToken(usernameOrEmail, "user", user.getId());
    }

    public KeycloakLoginResult loginWithKeycloak(String usernameOrEmail, String password, boolean rememberMe) {
        KeycloakService.TokenGrantResponse grant = keycloakService.login(usernameOrEmail, password, rememberMe);
        TokenResponse tokenResponse = createLoginResponse(grant.accessToken(), usernameOrEmail);
        tokenResponse.setExpiresIn(grant.expiresIn());
        return new KeycloakLoginResult(tokenResponse, grant.refreshToken());
    }

    public KeycloakLoginResult refreshWithKeycloak(String refreshToken, String usernameHint) {
        KeycloakService.TokenGrantResponse grant = keycloakService.refresh(refreshToken);
        TokenResponse tokenResponse = createLoginResponse(
                grant.accessToken(),
                usernameHint != null && !usernameHint.isBlank() ? usernameHint : extractUsername(grant.accessToken())
        );
        tokenResponse.setExpiresIn(grant.expiresIn());
        return new KeycloakLoginResult(tokenResponse, grant.refreshToken());
    }

    public String generateRefreshTokenByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Generating refresh token for user: " + usernameOrEmail);
        UserCredential user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return jwtService.generateRefreshToken(usernameOrEmail, "user");
    }
    
    /**
     * Generate refresh token with custom expiration based on rememberMe flag
     * @param usernameOrEmail - username or email
     * @param rememberMe - if true, token expires in 30 days; if false, expires in 7 days
     * @return JWT refresh token
     */
    public String generateRefreshTokenByUsernameOrEmail(String usernameOrEmail, boolean rememberMe) {
        logger.info("Generating refresh token for user: " + usernameOrEmail + " (rememberMe: " + rememberMe + ")");
        UserCredential user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        // If rememberMe is true, extend token expiration to 30 days; otherwise 7 days
        long expirationMillis = rememberMe 
            ? 30L * 24 * 60 * 60 * 1000  // 30 days
            : 7L * 24 * 60 * 60 * 1000;   // 7 days
            
        logger.info("Refresh token expiration set to: " + (rememberMe ? "30 days" : "7 days"));
        return jwtService.generateRefreshToken(usernameOrEmail, "user", expirationMillis);
    }

    public String getAccessTokenByRefreshToken(String refreshToken) {
        logger.info("Validating refresh token");
        
        // Validate token signature and expiration
        if (!jwtService.validateTokenWithExpiration(refreshToken)) {
            logger.warn("Invalid or expired refresh token");
            return null;
        }
        
        // Check if it's actually a refresh token
        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            logger.warn("Token is not a refresh token, type: " + tokenType);
            return null;
        }
        
        String username = jwtService.extractUsername(refreshToken);
        logger.info("Refresh token valid, generating new access token for user: " + username);
        return generateAccessTokenByUsernameOrEmail(username);
    }

    public boolean validateToken(String token) {
        logger.info("Validating token");
        return jwtService.validateToken(token);
    }

    public UserCredential findById(Long id){
        Optional<UserCredential> userCredential= userCredentialRepository.findById(id);
        return userCredential.orElse(null);
    }

    /**
     * Legacy method - kept for backward compatibility
     * @deprecated Use resetPasswordByToken instead
     */
    @Deprecated
    public boolean resetPassword(Long userId, String newPassword) {
        Optional<UserCredential> userOptional = userCredentialRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserCredential user = userOptional.get();
            if (user.getKeycloakUserId() == null || user.getKeycloakUserId().isBlank()) {
                return false;
            }
            keycloakService.resetUserPassword(user.getKeycloakUserId(), newPassword);
            return true;
        }
        return false;
    }

    /**
     * Request password reset - generates token and sends email
     */
    @Transactional
    public BaseResponse<String> requestPasswordReset(String email, String ipAddress, String userAgent) {
        logger.info("Password reset requested for email: " + email);
        
        KeycloakService.KeycloakUserProfile keycloakUser = keycloakService.findUserByEmail(email);
        if (keycloakUser == null) {
            logger.warn("Password reset requested for non-existent email: " + email);
            return new BaseResponse<>(false, null, 404, "No account found with this email address. Please check your email or sign up.", null);
        }

        UserCredential user = findUserByKeycloakUserId(keycloakUser.id());
        if (user == null) {
            logger.warn("Password reset requested but local profile is missing for keycloak user: " + keycloakUser.id());
            return new BaseResponse<>(false, null, 404, "No local account profile found for this user.", null);
        }
        
        // Invalidate any existing unused tokens for this user
        passwordResetTokenRepository.invalidateAllTokensForUser(user, LocalDateTime.now());
        
        // Generate secure token
        String token = UUID.randomUUID().toString();
        
        // Create expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);
        
        // Save token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setIsUsed(false);
        resetToken.setIpAddress(ipAddress);
        resetToken.setUserAgent(userAgent);
        resetToken.setCreatedAt(LocalDateTime.now());
        
        passwordResetTokenRepository.save(resetToken);
        String recipientEmail = keycloakUser.email() != null ? keycloakUser.email() : email;
        String displayName = buildDisplayName(keycloakUser);
        logger.info("Password reset token generated for user: " + recipientEmail + ", expires at: " + expiresAt);
        
        // Send email with reset link
        String resetLink = resetUrl + "?token=" + token;
        emailNotificationService.sendPasswordResetEmail(recipientEmail, displayName, resetLink, tokenExpirationMinutes, ipAddress);
        
        String successMessage = "Password reset instructions have been sent to your email address.";
        return new BaseResponse<>(true, successMessage, 0, null, null);
    }

    /**
     * Validate password reset token
     */
    public ValidateTokenResponse validateResetToken(String token) {
        logger.info("Validating password reset token");
        
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findValidToken(
            token, LocalDateTime.now());
        
        if (tokenOptional.isEmpty()) {
            logger.warn("Invalid or expired password reset token");
            return new ValidateTokenResponse(false, "Invalid or expired token", null);
        }
        
        PasswordResetToken resetToken = tokenOptional.get();
        long minutesRemaining = ChronoUnit.MINUTES.between(LocalDateTime.now(), resetToken.getExpiresAt());
        
        logger.info("Token is valid, expires in " + minutesRemaining + " minutes");
        return new ValidateTokenResponse(true, "Token is valid", minutesRemaining);
    }

    /**
     * Reset password using token
     */
    @Transactional
    public BaseResponse<String> resetPasswordByToken(String token, String newPassword) {
        logger.info("Password reset requested with token");
        
        // Validate token
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findValidToken(
            token, LocalDateTime.now());
        
        if (tokenOptional.isEmpty()) {
            logger.warn("Invalid or expired password reset token");
            return new BaseResponse<>(false, null, 0, "Invalid or expired token", null);
        }
        
        PasswordResetToken resetToken = tokenOptional.get();
        UserCredential user = resetToken.getUser();
        
        // Check password history - prevent reusing recent passwords
        List<PasswordHistory> recentPasswords = passwordHistoryRepository.findLastNPasswords(
            user.getId(), passwordHistoryCheckCount);
        
        for (PasswordHistory history : recentPasswords) {
            // Check if the new password matches any recent password
            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                logger.warn("User attempted to reuse a recent password. localUserId=" + user.getId());
                return new BaseResponse<>(false, null, 0, 
                    "You cannot reuse a password that you've used recently. Please choose a different password.", null);
            }
        }
        
        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        
        // Store old password in history before updating
        PasswordHistory oldPasswordHistory = new PasswordHistory();
        oldPasswordHistory.setUser(user);
        // Track password history locally using the newly chosen password hash.
        oldPasswordHistory.setPasswordHash(passwordEncoder.encode(newPassword));
        oldPasswordHistory.setChangedAt(LocalDateTime.now());
        passwordHistoryRepository.save(oldPasswordHistory);
        
        // Update password in Keycloak (source of truth for credentials)
        if (user.getKeycloakUserId() == null || user.getKeycloakUserId().isBlank()) {
            logger.error("Cannot reset password: missing keycloakUserId for local userId=" + user.getId());
            return new BaseResponse<>(false, null, 0, "Cannot reset password for this account. Contact support.", null);
        }
        keycloakService.resetUserPassword(user.getKeycloakUserId(), newPassword);
        
        logger.info("Password reset successful for local userId: " + user.getId());
        
        // Invalidate all other reset tokens for this user (security: prevent reuse)
        passwordResetTokenRepository.invalidateAllTokensForUser(user, LocalDateTime.now());
        
        // Invalidate all refresh tokens (session invalidation)
        // Since refresh tokens are JWT stored in cookies, we need to blacklist them
        // We'll blacklist by username - all tokens for this user will be invalidated
        try {
            // Extract username from user
            String username = user.getId() != null ? "user-" + user.getId() : "unknown-user";
            // Blacklist all existing refresh tokens by adding them to blacklist
            // Note: Since we can't enumerate all JWT tokens, we'll use a pattern-based approach
            // For now, we'll log that sessions should be invalidated
            // In a production system, you might want to track active refresh tokens in a database
            logger.info("All sessions for user " + username + " should be invalidated after password reset");
            
            // If you have a refresh token repository, invalidate them here
            // For JWT-based refresh tokens, clients will need to re-authenticate
            // The token blacklist service can be used if tokens are tracked
            
        } catch (Exception e) {
            logger.error("Error invalidating sessions: " + e.getMessage());
            // Don't fail the password reset if session invalidation fails
        }
        
        // Send confirmation email
        KeycloakService.KeycloakUserProfile keycloakUser = keycloakService.findUserById(user.getKeycloakUserId());
        if (keycloakUser != null && keycloakUser.email() != null) {
            emailNotificationService.sendPasswordResetConfirmationEmail(
                    keycloakUser.email(),
                    buildDisplayName(keycloakUser)
            );
        }
        
        return new BaseResponse<>(true, "Password successfully reset. Please sign in again.", 0, null, null);
    }

    /**
     * Send email verification token to user
     */
    private void sendEmailVerification(UserCredential user) {
        try {
            UserCredential managedUser = userCredentialRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + user.getId()));
            if (managedUser.getKeycloakUserId() == null || managedUser.getKeycloakUserId().isBlank()) {
                logger.error("Cannot trigger Keycloak verification email: missing keycloakUserId for local userId=" + managedUser.getId());
                return;
            }

            KeycloakService.KeycloakUserProfile keycloakUser = keycloakService.findUserById(managedUser.getKeycloakUserId());
            String recipientEmail = keycloakUser != null && keycloakUser.email() != null
                    ? keycloakUser.email()
                    : null;
            if (recipientEmail == null || recipientEmail.isBlank()) {
                logger.error("Cannot trigger Keycloak verification email: missing recipient for local userId=" + managedUser.getId());
                return;
            }

            keycloakService.sendVerifyEmail(managedUser.getKeycloakUserId());
            logger.info("Keycloak verification email triggered for user: " + recipientEmail);
        } catch (Exception e) {
            logger.error("Failed to trigger Keycloak verification email: " + e.getMessage());
            // Don't throw - allow registration to succeed even if email fails
        }
    }

    /**
     * Verify email address using token.
     * For salon owners, returns an access token so they can continue onboarding
     * (e.g., create a salon) immediately after verification.
     */
    @Transactional
    public BaseResponse<TokenResponse> verifyEmail(String token) {
        logger.info("Email verification endpoint called with legacy token flow while Keycloak manages verification.");
        return new BaseResponse<>(false, null, 410,
                "Email verification is handled by Keycloak. Please use the latest verification email and then sign in.",
                null);
    }

    /**
     * Resend email verification
     */
    @Transactional
    public BaseResponse<String> resendVerificationEmail(String email) {
        logger.info("Resend email verification requested for: " + email);

        KeycloakService.KeycloakUserProfile keycloakUser = keycloakService.findUserByEmail(email);
        if (keycloakUser == null) {
            logger.warn("Resend verification requested for non-existent email: " + email);
            return new BaseResponse<>(false, null, 404, "No account found with this email address.", null);
        }

        UserCredential user = findUserByKeycloakUserId(keycloakUser.id());
        if (user == null) {
            logger.warn("Resend verification requested but local profile missing for keycloak user: " + keycloakUser.id());
            return new BaseResponse<>(false, null, 404, "No local account profile found for this user.", null);
        }

        // Only salon owners require email verification
        boolean isOwner = keycloakUser.realmRoles() != null
                && keycloakUser.realmRoles().stream().anyMatch(r -> "owner".equalsIgnoreCase(r));

        if (!isOwner) {
            logger.warn("Resend verification requested for non-owner account: " + email);
            return new BaseResponse<>(false, null, 0, "Email verification is not required for this account.", null);
        }
        
        if (Boolean.TRUE.equals(keycloakUser.emailVerified())) {
            logger.info("Email already verified for user: " + email);
            return new BaseResponse<>(false, null, 0, "Email is already verified", null);
        }
        
        // Send new verification email
        sendEmailVerification(user);
        
        return new BaseResponse<>(true, "Verification email sent successfully", 0, null, null);
    }

    /**
     * Create login response with onboarding status (for owners)
     */
    public TokenResponse createLoginResponse(String accessToken, String usernameOrEmail) {
        String keycloakUserId = extractSubject(accessToken);
        UserCredential user = findUserByKeycloakUserId(keycloakUserId);
        if (user == null) {
            user = findUserByUsernameOrEmail(usernameOrEmail);
        }
        if (user == null) {
            return new TokenResponse(accessToken, 7200L);
        }

        // Build user payload for frontend
        LoginUserResponse userResponse = new LoginUserResponse();
        userResponse.setId(user.getId());
        KeycloakService.KeycloakUserProfile profile = extractSubject(accessToken) != null
                ? keycloakService.findUserById(extractSubject(accessToken))
                : keycloakService.findUserByEmail(usernameOrEmail);
        userResponse.setFullName(buildDisplayName(profile));

        String roleName = extractPrimaryRole(accessToken);
        if (roleName != null) {
            userResponse.setRole(roleName.toLowerCase());
        }

        String normalizedRole = roleName != null ? roleName.toLowerCase() : null;

        // Owners: include onboarding status and, if available, salon info
        if ("owner".equalsIgnoreCase(normalizedRole)) {
            Boolean emailVerified = extractEmailVerified(accessToken);
            OnboardingStatusResponse onboardingStatus = onboardingService.getOnboardingStatus(user.getId(), emailVerified, true);

            // Try to fetch salon publicId for this owner
            String saloonId = salonClient.getSalonPublicIdForOwner(user.getId());
            if (saloonId != null) {
                userResponse.setSaloonId(saloonId);

                // Derive a simple salon status from onboarding progress
                String salonStatus = Boolean.TRUE.equals(onboardingStatus.getIsOnboardingComplete())
                        ? "active"
                        : "pending_setup";
                userResponse.setSalonStatus(salonStatus);
            }

            return new TokenResponse(accessToken, 7200L, userResponse, onboardingStatus);
        }

        // Non-owners: return token + user without onboarding
        return new TokenResponse(accessToken, 7200L, userResponse, null);
    }

    public boolean updateLocation(AddLocationRequest addLocationRequest) {
        Optional<UserCredential> userOptional = userCredentialRepository.findById(addLocationRequest.getUserId());
        if (userOptional.isPresent()) {
            UserCredential user = userOptional.get();
            user.setLatitude(addLocationRequest.getLatitude());
            user.setLongitude(addLocationRequest.getLongitude());
            userCredentialRepository.save(user);
            return true;
        }
        return false;
    }

    public OnboardingStatusResponse getOnboardingStatusForUser(String keycloakUserId, String usernameOrEmail) {
        UserCredential user = findUserByKeycloakUserId(keycloakUserId);
        if (user == null) {
            user = findUserByUsernameOrEmail(usernameOrEmail);
        }
        if (user == null) {
            return null;
        }
        KeycloakService.KeycloakUserProfile keycloakUser = keycloakService.findUserById(user.getKeycloakUserId());
        if (keycloakUser == null && usernameOrEmail != null && !usernameOrEmail.isBlank()) {
            keycloakUser = keycloakService.findUserByEmail(usernameOrEmail);
        }
        if (keycloakUser == null) {
            return onboardingService.getOnboardingStatus(user.getId());
        }
        boolean isOwner = keycloakUser.realmRoles() != null
                && keycloakUser.realmRoles().stream().anyMatch(r -> "owner".equalsIgnoreCase(r));
        return onboardingService.getOnboardingStatus(user.getId(), keycloakUser.emailVerified(), isOwner);
    }

    private UserCredential ensureLocalUserProfile(String keycloakUserId) {
        UserCredential existing = findUserByKeycloakUserId(keycloakUserId);
        if (existing != null) {
            return existing;
        }
        UserCredential user = new UserCredential();
        user.setKeycloakUserId(keycloakUserId);
        user.setOnboardingStatus("{}");
        return userCredentialRepository.save(user);
    }

    private String mapRoleNameForKeycloak(String roleName) {
        if (roleName == null) {
            return null;
        }
        return roleName.trim().toLowerCase();
    }

    private String extractUsername(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode parseJwtPayload(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(decoded);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractSubject(String token) {
        JsonNode payload = parseJwtPayload(token);
        if (payload == null) {
            return null;
        }
        JsonNode sub = payload.get("sub");
        return sub != null && !sub.isNull() ? sub.asText() : null;
    }

    private Boolean extractEmailVerified(String token) {
        JsonNode payload = parseJwtPayload(token);
        if (payload == null) {
            return null;
        }
        JsonNode emailVerified = payload.get("email_verified");
        return emailVerified != null && !emailVerified.isNull() ? emailVerified.asBoolean() : null;
    }

    private String extractPrimaryRole(String token) {
        JsonNode payload = parseJwtPayload(token);
        if (payload == null) {
            return null;
        }
        JsonNode realmAccess = payload.get("realm_access");
        if (realmAccess == null || realmAccess.isNull()) {
            return null;
        }
        JsonNode roles = realmAccess.get("roles");
        if (roles == null || !roles.isArray() || roles.isEmpty()) {
            return null;
        }
        for (JsonNode role : roles) {
            String value = role.asText();
            if ("owner".equalsIgnoreCase(value)) {
                return "owner";
            }
        }
        return roles.get(0).asText();
    }

    private String buildDisplayName(KeycloakService.KeycloakUserProfile profile) {
        if (profile == null) {
            return "User";
        }
        String firstName = profile.firstName() != null ? profile.firstName().trim() : "";
        String lastName = profile.lastName() != null ? profile.lastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        if (profile.username() != null && !profile.username().isBlank()) {
            return profile.username();
        }
        if (profile.email() != null && !profile.email().isBlank()) {
            return profile.email();
        }
        return "User";
    }

    public record KeycloakLoginResult(TokenResponse tokenResponse, String refreshToken) {
    }
}
