package com.baber.identityservice.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data 
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthRequest {

    /**
     * Can be either username or email address
     * Accepts JSON fields: "usernameOrEmail", "email", or "username"
     */
    @JsonAlias({"email", "username"})  // Also accept "email" or "username" from JSON
    private String usernameOrEmail;
    
    private String password;
    
    /**
     * If true, user wants to stay logged in for extended period (30 days)
     * If false or null, standard session duration (7 days)
     */
    private Boolean rememberMe;

}
