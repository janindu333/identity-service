package com.baber.identityservice.identityservice.service;

import com.baber.identityservice.identityservice.dto.AddLocationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baber.identityservice.identityservice.dto.BaseResponse;
import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.UserCredentialRepository;
import reactor.core.publisher.Mono;
@Service
public class AuthService {
    @Autowired
    private UserCredentialRepository userCredentialRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    public Mono<BaseResponse<Object>> saveUser(UserCredential userCredential) {
        return userCredentialRepository.findByName(userCredential.getName())
                .flatMap(existingUser ->
                        Mono.just(
                        new BaseResponse<>(false, null, 0,
                        "user name already exists", null)))
                .switchIfEmpty(userCredentialRepository.findByEmail(userCredential.getEmail())
                        .flatMap(existingUser -> {
                            if (existingUser != null) {
                                return Mono.just(new BaseResponse<>(false, null, 0,
                                        "user email already exists", null));
                            } else {
                                // Encode the user's password before saving
                                userCredential.setPassword(passwordEncoder.encode(userCredential.getPassword()));
                                return userCredentialRepository.save(userCredential)
                                        .map(savedUser -> new BaseResponse<>(true, "user added", 0,
                                                null, null))
                                        .onErrorResume(e -> {
                                            e.printStackTrace(); // Consider replacing this with appropriate logging
                                            return Mono.just(new BaseResponse<>(false, null, 0,
                                                    "user addition failed: " + e.getMessage(), null));
                                        });
                            }
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            // Encode the user's password before saving
                            userCredential.setPassword(passwordEncoder.encode(userCredential.getPassword()));
                            return userCredentialRepository.save(userCredential)
                                    .map(savedUser ->
                                            new BaseResponse<>(true, "user added", 0,
                                            null, null))
                                    .onErrorResume(e -> {
                                        e.printStackTrace(); // Consider replacing this with appropriate logging
                                        return Mono.just(new BaseResponse<>(false, null, 0,
                                                "user addition failed: " + e.getMessage(), null));
                                    });
                        }))
                )
                .onErrorResume(e -> {
                    e.printStackTrace(); // Consider replacing this with appropriate logging
                    return Mono.just(new BaseResponse<>(false, null, 0,
                            "user addition failed: " + e.getMessage(), null));
                });
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
    public Mono<UserCredential> findById(int id){
        return userCredentialRepository.findById(id);
    }
    public Mono<Boolean> resetPassword(int id, String password) {
        // Step 1: Retrieve the user from the database using the given ID
        Mono<UserCredential> userMono = userCredentialRepository.findById(id);

        // Step 2: Perform operations on the user asynchronously
        return userMono.flatMap(user -> {
            // Step 3: Update the password of the retrieved user
            user.setPassword(passwordEncoder.encode(password));

            // Step 4: Save the updated user back to the database
            return userCredentialRepository.save(user)
                    .thenReturn(true); // Return true after successful save
        }).switchIfEmpty(
                Mono.just(false)
        ); // Return false if user not found
    }
    public Mono<Boolean> updateLocation(AddLocationRequest addLocationRequest){

        // Step 1: Retrieve the user from the database using the given ID
        Mono<UserCredential> userMono = userCredentialRepository.findById(addLocationRequest.getUserId());

        // Step 2: Perform operations on the user asynchronously
        return userMono.flatMap(user -> {
            // Check if the user exists with the given ID
            if (user == null) {
                return Mono.just(false);
            }

            // Step 3: Update the location of the retrieved user
            user.setLatitude(addLocationRequest.getLatitude());
            user.setLongitude(addLocationRequest.getLongitude());

            // Step 4: Save the updated user back to the database
            return userCredentialRepository.save(user).map(savedUser -> true);
        }).defaultIfEmpty(false); // Return false if no user found
    }
    public Mono<BaseResponse<UserCredential>> getUserCredentialByUsername(String username) {
        return userCredentialRepository.findByName(username)
                .flatMap(existingUser ->
                        Mono.just(
                        new BaseResponse<>(true, "User found", 0,
                        null, existingUser)))
                .switchIfEmpty(
                        Mono.just(
                        new BaseResponse<>(false, null, 0,
                        "User not found with username: " + username, null)))
                .onErrorResume(e -> {
                    e.printStackTrace(); // Consider replacing this with appropriate logging
                    return Mono.just(new BaseResponse<>(false, null, 0,
                            "Failed to retrieve user: " + e.getMessage(), null));
                });
    }


}
