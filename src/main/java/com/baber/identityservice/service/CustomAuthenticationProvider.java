package com.baber.identityservice.service;

import com.baber.identityservice.entity.UserCredential;
import com.baber.identityservice.repository.UserCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usernameOrEmail = authentication.getName();
        String password = authentication.getCredentials() != null 
            ? authentication.getCredentials().toString() 
            : "";

        logger.info("Attempting authentication for: {}", usernameOrEmail);

        Optional<UserCredential> userByName = userCredentialRepository.findByName(usernameOrEmail);
        Optional<UserCredential> userByEmail = userCredentialRepository.findByEmail(usernameOrEmail);

        logger.debug("User by name present: {}", userByName.isPresent());
        logger.debug("User by email present: {}", userByEmail.isPresent());

        UserCredential user = userByName.orElseGet(() -> userByEmail.orElse(null));

        if (user == null) {
            logger.warn("User not found for: {}", usernameOrEmail);
            throw new BadCredentialsException("Invalid username/email or password");
        }

        // Validate password using PasswordEncoder
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Invalid password for user: {}", usernameOrEmail);
            throw new BadCredentialsException("Invalid username/email or password");
        }

        String roleName = (user.getRole() != null && user.getRole().getName() != null)
            ? user.getRole().getName()
            : "USER";

        logger.info("Authentication successful for user: {}", user.getName());
        return new UsernamePasswordAuthenticationToken(
            user.getName(),
            password,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
} 