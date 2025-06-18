package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.UserCredentialRepository;
import com.baber.identityservice.identityservice.config.ServiceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final ServiceLogger logger = new ServiceLogger(AuthService.class);

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public BaseResponse<String> saveUser(UserCredential userCredential) {
        logger.info("Saving user: " + userCredential.getName());
        try {
            Optional<UserCredential> existingUserByName = userCredentialRepository.findByName(userCredential.getName());
            if (existingUserByName.isPresent()) {
                logger.warn("User with name: " + userCredential.getName() + " already exists");
                return new BaseResponse<>(false, null, 0, "User name already exists", null);
            }

            Optional<UserCredential> existingUserByEmail = userCredentialRepository.findByEmail(userCredential.getEmail());
            if (existingUserByEmail.isPresent()) {
                logger.warn("User with email: " + userCredential.getEmail() + " already exists");
                return new BaseResponse<>(false, null, 0, "User email already exists", null);
            }

            userCredential.setPassword(passwordEncoder.encode(userCredential.getPassword()));
            userCredential.setRole("USER"); // Default role
            userCredentialRepository.save(userCredential);
            logger.success("User saved successfully: " + userCredential.getName());
            return new BaseResponse<>(true, "User added", 0, null, null);
        } catch (Exception e) {
            logger.error("Error saving user: " + userCredential.getName() + ", Error: " + e.getMessage());
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
        return jwtService.generateToken(username, user.getRole(), 120 * 60 * 1000); // 2 hours
    }

    public String generateRefreshToken(String username) {
        logger.info("Generating refresh token for user: " + username);
        UserCredential user = userCredentialRepository.findByName(username)
                .orElseThrow(() -> {
                    logger.error("User not found: " + username);
                    return new RuntimeException("User not found");
                });
        return jwtService.generateToken(username, user.getRole(), 7 * 24 * 60 * 60 * 1000); // 7 days
    }

    public String getAccessTokenByRefreshToken(String refreshToken) {
        logger.info("Validating refresh token");
        if (jwtService.validateToken(refreshToken)) {
            String username = jwtService.extractUsername(refreshToken);
            logger.info("Refresh token valid, generating new access token for user: " + username);
            return generateAccessToken(username);
        } else {
            logger.warn("Invalid refresh token");
            return null;
        }
    }

    public boolean validateToken(String token) {
        logger.info("Validating token");
        return jwtService.validateToken(token);
    }

    public UserCredential findById(int id) {
        logger.info("Finding user by ID: " + id);
        Optional<UserCredential> userCredential = userCredentialRepository.findById(id);
        return userCredential.orElse(null);
    }

    public boolean resetPassword(int id, String password) {
        logger.info("Resetting password for user ID: " + id);
        Optional<UserCredential> optionalUser = userCredentialRepository.findById(id);

        if (optionalUser.isEmpty()) {
            logger.warn("User not found with ID: " + id);
            return false;
        }

        UserCredential user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(password));
        userCredentialRepository.save(user);
        logger.info("Password reset successfully for user ID: " + id);
        return true;
    }

    public boolean updateLocation(AddLocationRequest addLocationRequest) {
        logger.info("Updating location for user ID: " + addLocationRequest.getUserId());
        Optional<UserCredential> optionalUser = userCredentialRepository.findById(addLocationRequest.getUserId());

        if (optionalUser.isEmpty()) {
            logger.warn("User not found for ID: " + addLocationRequest.getUserId());
            return false;
        }

        UserCredential user = optionalUser.get();
        user.setLatitude(addLocationRequest.getLatitude());
        user.setLongitude(addLocationRequest.getLongitude());
        userCredentialRepository.save(user);
        logger.info("Location updated successfully for user ID: " + addLocationRequest.getUserId());
        return true;
    }
}
