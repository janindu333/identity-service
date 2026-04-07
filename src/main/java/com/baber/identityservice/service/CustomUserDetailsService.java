package com.baber.identityservice.service;

import com.baber.identityservice.entity.UserCredential;
import com.baber.identityservice.repository.UserCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.info("Loading user details for: {}", usernameOrEmail);
        
        // Try to find by name first, then by email
        UserCredential userCredential = userCredentialRepository.findByName(usernameOrEmail)
                .orElseGet(() -> userCredentialRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> {
                            logger.warn("User not found: {}", usernameOrEmail);
                            return new UsernameNotFoundException("User not found: " + usernameOrEmail);
                        }));

        // Create UserDetails with authorities based on user role
        UserDetails userDetails = User.builder()
                .username(userCredential.getName())
                .password(userCredential.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userCredential.getRole().getName())))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();

        logger.info("User details loaded successfully for: {}", usernameOrEmail);
        return userDetails;
    }
} 