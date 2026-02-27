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
import com.baber.identityservice.entity.EmailVerificationToken;
import com.baber.identityservice.repository.UserCredentialRepository;
import com.baber.identityservice.repository.RoleRepository;
import com.baber.identityservice.repository.PasswordResetTokenRepository;
import com.baber.identityservice.repository.PasswordHistoryRepository;
import com.baber.identityservice.repository.EmailVerificationTokenRepository;
import com.baber.identityservice.dto.OnboardingStatusResponse;
import com.baber.identityservice.config.ServiceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.baber.identityservice.dto.AddLocationRequest;

@Service
public class AuthService {
    private final ServiceLogger logger = new ServiceLogger(AuthService.class);

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
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private SalonClient salonClient;

    @Value("${app.password-reset.token-expiration-minutes:60}")
    private int tokenExpirationMinutes;

    @Value("${app.password-reset.reset-url:http://localhost:3000/reset-password}")
    private String resetUrl;

    @Value("${app.email-verification.token-expiration-minutes:1440}")
    private int emailVerificationTokenExpirationMinutes; // Default 24 hours

    @Value("${app.email-verification.verify-url:http://localhost:3000/verify-email}")
    private String emailVerificationUrl;

    @Value("${app.password-history.check-count:5}")
    private int passwordHistoryCheckCount; // Number of recent passwords to check

    @Transactional
    public RegistrationResult saveUser(UserRegistrationRequest request) {
        logger.info("Saving user: " + request.getFirstName() + " " + request.getLastName());
        try {
            // Check if user exists by email
            Optional<UserCredential> existingUserByEmail = userCredentialRepository
            .findByEmail(request.getEmail());
            if (existingUserByEmail.isPresent()) {
                UserCredential existingUser = existingUserByEmail.get();

                // If the existing account is not verified (typically for salon owners),
                // follow industry best practice:
                // - Do NOT create a new account
                // - Re-send the verification email
                // - Return success with a clear message so the frontend can guide the user
                if (Boolean.FALSE.equals(existingUser.getEmailVerified())) {
                    logger.warn("Registration attempted with email that already exists but is not verified: "
                            + request.getEmail());

                    // Only owners (roleId = 2) require verification / onboarding
                    Role existingRole = existingUser.getRole();
                    Integer existingRoleId = existingRole != null ? existingRole.getId() : null;
                    boolean existingIsOwner = existingRoleId != null && existingRoleId == 2;

                    if (existingIsOwner) {
                        // Send (or re-send) verification email for the existing owner account
                        sendEmailVerification(existingUser);

                        return new RegistrationResult(
                            true,
                            "An account with this email already exists but is not verified. " +
                            "We have sent you a new verification email. Please check your inbox.",
                            0,
                            null
                        );
                    }

                    // For any other role with unverified email, keep behavior simple:
                    // treat it as a conflict to avoid unexpected flows.
                    logger.warn("Existing unverified account is not an owner role. Treating as conflict.");
                    return new RegistrationResult(false, null, 409, "User email already exists");
                }

                logger.warn("User with email: " + request.getEmail()
                + " already exists and is verified");
                return new RegistrationResult(false, null, 409, "User email already exists");
            }

            // Check if user exists by phone
            Optional<UserCredential> existingUserByPhone = userCredentialRepository
            .findByPhone(request.getPhone());
            if (existingUserByPhone.isPresent()) {
                logger.warn("User with phone: " + request.getPhone() 
                + " already exists");
                return new RegistrationResult(false, null, 409, "User phone already exists");
            }
    
            // Get the role from database
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
    
            // Convert DTO to entity
            UserCredential userCredential = new UserCredential();
            userCredential.setFirstName(request.getFirstName());
            userCredential.setLastName(request.getLastName());
            userCredential.setEmail(request.getEmail());
            userCredential.setPhone(request.getPhone());
            userCredential.setPassword(passwordEncoder.encode(request.getPassword()));
            userCredential.setRole(userRole); // Set the Role entity

            // Determine behavior based on role:
            // - Salon Owner (roleId = 2): email verification REQUIRED
            // - Client/Customer (roleId = 7): email verification NOT required, active immediately
            int roleId = userRole.getId();
            boolean isOwner = roleId == 2;
            boolean isClient = roleId == 7;

            if (isOwner) {
                // Owners must verify email
                userCredential.setEmailVerified(false);
            } else if (isClient) {
                // Clients are active immediately
                userCredential.setEmailVerified(true);
            } else {
                // Fallback: treat unknown roles as requiring verification for safety
                userCredential.setEmailVerified(false);
            }
    
            userCredentialRepository.save(userCredential);
            logger.success("User saved successfully: " + userCredential.getName() + " with role: " + userRole.getName());
            
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
            logger.error("User not found: " + username);
            throw new RuntimeException("User not found");
        }
        return jwtService.generateAccessToken(username, user.getRole().getName(), user.getId(), user.getRole().getId());
    }

    public String generateRefreshToken(String username) {
        logger.info("Generating refresh token for user: " + username);
        UserCredential user = findUserByUsernameOrEmail(username);
        if (user == null) {
            logger.error("User not found: " + username);
            throw new RuntimeException("User not found");
        }
        return jwtService.generateRefreshToken(username, user.getRole().getName());
    }

    public UserCredential findUserByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Finding user by username or email: " + usernameOrEmail);
        
        // First try to find by full name (firstName + lastName)
        Optional<UserCredential> userByName = userCredentialRepository
        .findByName(usernameOrEmail);
        if (userByName.isPresent()) {
            logger.info("User found by full name: " + usernameOrEmail);
            return userByName.get();
        }
        
        // If not found by full name, try to find by email
        Optional<UserCredential> userByEmail = userCredentialRepository.findByEmail(usernameOrEmail);
        if (userByEmail.isPresent()) {
            logger.info("User found by email: " + usernameOrEmail);
            return userByEmail.get();
        }
        
        logger.warn("User not found by username or email: " + usernameOrEmail);
        return null;
    }

    public String generateAccessTokenByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Generating access token for user: " + usernameOrEmail);
        UserCredential user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // Use the method with 2 hours expiration (120 * 60 * 1000 milliseconds)
        return jwtService.generateAccessToken(user.getName(), user.getRole().getName(), user.getId());
    }

    public String generateRefreshTokenByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Generating refresh token for user: " + usernameOrEmail);
        UserCredential user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // Use the method with 7 days expiration and include roleId
        return jwtService.generateRefreshToken(user.getName(), user.getRole().getName());
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
        return jwtService.generateRefreshToken(user.getName(), user.getRole().getName(), expirationMillis);
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
            user.setPassword(passwordEncoder.encode(newPassword));
            userCredentialRepository.save(user);
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
        
        Optional<UserCredential> userOptional = userCredentialRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warn("Password reset requested for non-existent email: " + email);
            return new BaseResponse<>(false, null, 404, "No account found with this email address. Please check your email or sign up.", null);
        }

        UserCredential user = userOptional.get();
        
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
        logger.info("Password reset token generated for user: " + user.getEmail() + ", expires at: " + expiresAt);
        
        // Send email with reset link
        String resetLink = resetUrl + "?token=" + token;
        emailNotificationService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink, tokenExpirationMinutes, ipAddress);
        
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
                logger.warn("User attempted to reuse a recent password: " + user.getEmail());
                return new BaseResponse<>(false, null, 0, 
                    "You cannot reuse a password that you've used recently. Please choose a different password.", null);
            }
        }
        
        // Also check if new password matches current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            logger.warn("User attempted to reuse current password: " + user.getEmail());
            return new BaseResponse<>(false, null, 0, 
                "New password must be different from your current password.", null);
        }
        
        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        
        // Store old password in history before updating
        PasswordHistory oldPasswordHistory = new PasswordHistory();
        oldPasswordHistory.setUser(user);
        oldPasswordHistory.setPasswordHash(user.getPassword()); // Store the old hashed password
        oldPasswordHistory.setChangedAt(LocalDateTime.now());
        passwordHistoryRepository.save(oldPasswordHistory);
        
        // Update password with new hash
        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.setPassword(newPasswordHash);
        userCredentialRepository.save(user);
        
        logger.info("Password reset successful for user: " + user.getEmail());
        
        // Invalidate all other reset tokens for this user (security: prevent reuse)
        passwordResetTokenRepository.invalidateAllTokensForUser(user, LocalDateTime.now());
        
        // Invalidate all refresh tokens (session invalidation)
        // Since refresh tokens are JWT stored in cookies, we need to blacklist them
        // We'll blacklist by username - all tokens for this user will be invalidated
        try {
            // Extract username from user
            String username = user.getName();
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
        emailNotificationService.sendPasswordResetConfirmationEmail(user.getEmail(), user.getName());
        
        return new BaseResponse<>(true, "Password successfully reset. Please sign in again.", 0, null, null);
    }

    /**
     * Send email verification token to user
     */
    private void sendEmailVerification(UserCredential user) {
        try {
            // Ensure user is managed in the current persistence context
            // Refresh to ensure it's attached (in case it was detached)
            UserCredential managedUser = userCredentialRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + user.getId()));
            
            // Invalidate any existing unused tokens for this user
            emailVerificationTokenRepository.invalidateAllTokensForUser(managedUser, LocalDateTime.now());
            
            // Generate secure token
            String token = UUID.randomUUID().toString();
            
            // Create expiration time
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(emailVerificationTokenExpirationMinutes);
            
            // Save token
            EmailVerificationToken verificationToken = new EmailVerificationToken();
            verificationToken.setToken(token);
            verificationToken.setUser(managedUser);
            verificationToken.setExpiresAt(expiresAt);
            verificationToken.setIsUsed(false);
            verificationToken.setCreatedAt(LocalDateTime.now());
            
            emailVerificationTokenRepository.save(verificationToken);
            logger.info("Email verification token generated for user: " + managedUser.getEmail() + ", expires at: " + expiresAt);
            
            // Send email with verification link
            String verificationLink = emailVerificationUrl + "?token=" + token;
            emailNotificationService.sendVerificationEmail(
                managedUser.getEmail(), 
                managedUser.getName(), 
                verificationLink, 
                emailVerificationTokenExpirationMinutes
            );
        } catch (Exception e) {
            logger.error("Failed to send email verification: " + e.getMessage());
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
        logger.info("Email verification requested with token");
        
        Optional<EmailVerificationToken> tokenOptional = emailVerificationTokenRepository.findValidToken(
            token, LocalDateTime.now()
        );
        
        if (tokenOptional.isEmpty()) {
            logger.warn("Invalid or expired email verification token");
            return new BaseResponse<>(false, null, 0,
                    "Invalid or expired verification token. Please request a new verification email.", null);
        }
        
        EmailVerificationToken verificationToken = tokenOptional.get();
        UserCredential user = verificationToken.getUser();
        
        // Ensure this flow is only used for owner accounts (roleId = 2)
        Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
        boolean isOwner = roleId != null && roleId == 2;

        if (!isOwner) {
            logger.warn("Email verification attempted for non-owner account: " + user.getEmail());
            return new BaseResponse<>(false, null, 0,
                    "Email verification is only required for salon owner accounts.", null);
        }

        // Check if email is already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            logger.info("Email already verified for user: " + user.getEmail());
            // Mark token as used anyway
            verificationToken.setIsUsed(true);
            verificationToken.setUsedAt(LocalDateTime.now());
            emailVerificationTokenRepository.save(verificationToken);

            // Even if already verified, issue an access token so the owner can continue onboarding
            String accessToken = generateAccessTokenByUsernameOrEmail(user.getEmail());
            TokenResponse tokenResponse = createLoginResponse(accessToken, user.getEmail());
            return new BaseResponse<>(true, "Email is already verified", 0, null, tokenResponse);
        }
        
        // Mark email as verified
        user.setEmailVerified(true);
        userCredentialRepository.save(user);
        
        // Mark email verification step as complete (for owners)
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        if ("owner".equalsIgnoreCase(roleName)) {
            onboardingService.completeEmailVerification(user.getId());
        }
        
        // Mark token as used
        verificationToken.setIsUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        // Generate access token so the owner can immediately proceed (e.g., create salon)
        String accessToken = generateAccessTokenByUsernameOrEmail(user.getEmail());
        TokenResponse tokenResponse = createLoginResponse(accessToken, user.getEmail());

        logger.info("Email verified successfully for user: " + user.getEmail());
        return new BaseResponse<>(true, "Email verified successfully", 0, null, tokenResponse);
    }

    /**
     * Resend email verification
     */
    @Transactional
    public BaseResponse<String> resendVerificationEmail(String email) {
        logger.info("Resend email verification requested for: " + email);
        
        Optional<UserCredential> userOptional = userCredentialRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warn("Resend verification requested for non-existent email: " + email);
            return new BaseResponse<>(false, null, 404, "No account found with this email address.", null);
        }
        
        UserCredential user = userOptional.get();

        // Only salon owners (roleId = 2) require email verification
        Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
        boolean isOwner = roleId != null && roleId == 2;

        if (!isOwner) {
            logger.warn("Resend verification requested for non-owner account: " + email);
            return new BaseResponse<>(false, null, 0, "Email verification is not required for this account.", null);
        }
        
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
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
        UserCredential user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            return new TokenResponse(accessToken, 7200L);
        }

        // Build user payload for frontend
        LoginUserResponse userResponse = new LoginUserResponse();
        userResponse.setId(user.getId());
        userResponse.setFullName(user.getName());

        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        if (roleName != null) {
            userResponse.setRole(roleName.toLowerCase());
        }

        String normalizedRole = roleName != null ? roleName.toLowerCase() : null;

        // Owners: include onboarding status and, if available, salon info
        if ("owner".equalsIgnoreCase(normalizedRole)) {
            OnboardingStatusResponse onboardingStatus = onboardingService.getOnboardingStatus(user.getId());

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
}
