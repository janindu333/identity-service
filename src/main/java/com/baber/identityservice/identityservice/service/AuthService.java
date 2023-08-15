package com.baber.identityservice.identityservice.service;

import java.util.Optional;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.UserCredentialRepository;
@Service
public class AuthService {
    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public BaseResponse<String> saveUser(UserCredential userCredential) {

        try {
            Optional<UserCredential> existingUserByName = userCredentialRepository.findByName(userCredential.getName());
            if (existingUserByName.isPresent()) {
                return new BaseResponse<>(false, null, 0, "user name already exist",null);
            } else {

                Optional<UserCredential> existingUserByEmail = userCredentialRepository
                        .findByEmail(userCredential.getEmail());
                if (existingUserByEmail.isPresent()) {
                    return new BaseResponse<>(false, null, 0, "user email already exist", null);

                } else {
                    userCredential.setPassword(passwordEncoder.encode(userCredential.getPassword()));
                    userCredentialRepository.save(userCredential);
                    return new BaseResponse<>(true, "user added", 0, null, null);

                }
            }
        } catch (Exception e) {

            e.printStackTrace();
            return new BaseResponse<>(false, null, 0, "user added failed, please try again", null);
        }

    }
    public String generateAccessToken(String username) {
        return jwtService.generateToken(username, 30 * 60 * 1000); // Set token expiration to 30 minutes
    }
    public String generateRefreshToken(String username) {
        return jwtService.generateToken(username, 7 * 24 * 60 * 60 * 1000); // Set token expiration to 7 days
    }
    public String getAccessTokenByRefreshToken(String refreshToken) {
        // Validate the refresh token using JwtService
        if (jwtService.validateToken(refreshToken)) {
            // Extract the username from the refresh token
            String username = jwtService.extractUsername(refreshToken);
            // Generate a new access token for the user
            return generateAccessToken(username);
        }
        return null; // Return null if the refresh token is invalid
    }
    public boolean validateToken(String token) {
      
        return jwtService.validateToken(token); 
    }
    public UserCredential findById(int id){
        Optional<UserCredential> userCredential= userCredentialRepository.findById(id);
        return userCredential.orElse(null);
    }

    public boolean resetPassword(int id, String password){

        // Step 1: Retrieve the user from the database using the given ID
        Optional<UserCredential> optionalUser = userCredentialRepository.findById(id);

        // Check if the user exists with the given ID
        if (optionalUser.isEmpty()) {
            return false;
        }

        UserCredential user = optionalUser.get();

        // Step 2: Update the password of the retrieved user
        user.setPassword(passwordEncoder.encode(password));

        // Step 3: Save the updated user back to the database
        userCredentialRepository.save(user);

        return true;
    }
    public boolean updateLocation(AddLocationRequest addLocationRequest){

        Optional<UserCredential> optionalUser = userCredentialRepository.findById(addLocationRequest.getUserId());

        if (optionalUser.isEmpty()) {
            return false;
        }

        UserCredential user = optionalUser.get();

        user.setLatitude(addLocationRequest.getLatitude());
        user.setLongitude(addLocationRequest.getLongitude());

        userCredentialRepository.save(user);

        return true;
    }
}
