package com.baber.identityservice.service;

import com.baber.identityservice.dto.BaseResponse;
import com.baber.identityservice.dto.UserRegistrationRequest;
import com.baber.identityservice.entity.UserCredential;
import com.baber.identityservice.entity.Role;
import com.baber.identityservice.repository.UserCredentialRepository;
import com.baber.identityservice.repository.RoleRepository;
import com.baber.identityservice.config.ServiceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
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

    public BaseResponse<String> saveUser(UserRegistrationRequest request) {
        logger.info("Saving user: " + request.getName());
        try {
            Optional<UserCredential> existingUserByName = userCredentialRepository
            .findByName(request.getName());
            if (existingUserByName.isPresent()) {
                logger.warn("User with name: " + request.getName() 
                + " already exists");
                return new BaseResponse<>(false, null, 
                0, "User name already exists", null);
            }
    
            Optional<UserCredential> existingUserByEmail = userCredentialRepository
            .findByEmail(request.getEmail());
            if (existingUserByEmail.isPresent()) {
                logger.warn("User with email: " + request.getEmail() 
                + " already exists");
                return new BaseResponse<>(false, null, 
                0, "User email already exists", null);
            }
    
            // Get the role from database
            Role userRole;
            if (request.getRoleId() != null) {
                Optional<Role> requestedRole = roleRepository.findById(request.getRoleId());
                if (requestedRole.isPresent()) {
                    userRole = requestedRole.get();
                } else {
                    return new BaseResponse<>(false, null, 0, "Invalid role ID provided", null);
                }
            } else {
                // Default to Customer role (ID: 7 based on your database)
                Optional<Role> defaultRole = roleRepository.findByName("Customer");
                if (defaultRole.isPresent()) {
                    userRole = defaultRole.get();
                } else {
                    return new BaseResponse<>(false, null, 0, "Default role not found in system", null);
                }
            }
    
            // Convert DTO to entity
            UserCredential userCredential = new UserCredential();
            userCredential.setName(request.getName());
            userCredential.setEmail(request.getEmail());
            userCredential.setPassword(passwordEncoder.encode(request.getPassword()));
            userCredential.setRole(userRole); // Set the Role entity
            userCredential.setLatitude(request.getLatitude());
            userCredential.setLongitude(request.getLongitude());
    
            userCredentialRepository.save(userCredential);
            logger.success("User saved successfully: " + userCredential.getName() + " with role: " + userRole.getName());
            return new BaseResponse<>(true, "User added", 0, null, null);
        } catch (Exception e) {
            logger.error("Error saving user: " + request.getName() + ", Error: " + e.getMessage());
            return new BaseResponse<>(false, null, 0, "User registration failed, please try again later", null);
        }
    }

    public String generateAccessToken(String username) {
        logger.info("Generating access token for user: " + username);
        UserCredential user = userCredentialRepository.findByName(username)
                .orElseThrow(() -> {
                    logger.error("User not found: " + username);
                    return new RuntimeException("User not found");
                });
        return jwtService.generateAccessToken(username, user.getRole().getName(), user.getId(), user.getRole().getId());
    }

    public String generateRefreshToken(String username) {
        logger.info("Generating refresh token for user: " + username);
        UserCredential user = userCredentialRepository.findByName(username)
                .orElseThrow(() -> {
                    logger.error("User not found: " + username);
                    return new RuntimeException("User not found");
                });
        return jwtService.generateRefreshToken(username, user.getRole().getName());
    }

    public UserCredential findUserByUsernameOrEmail(String usernameOrEmail) {
        logger.info("Finding user by username or email: " + usernameOrEmail);
        
        // First try to find by username
        Optional<UserCredential> userByName = userCredentialRepository
        .findByName(usernameOrEmail);
        if (userByName.isPresent()) {
            logger.info("User found by username: " + usernameOrEmail);
            return userByName.get();
        }
        
        // If not found by username, try to find by email
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
