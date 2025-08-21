package com.baber.identityservice.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data 
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthRequest {

    /**
     * Can be either username or email address
     */
    private String usernameOrEmail;
    private String password;

}
