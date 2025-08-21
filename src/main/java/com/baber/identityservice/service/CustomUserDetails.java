package com.baber.identityservice.service;

import com.baber.identityservice.entity.UserCredential;
import com.baber.identityservice.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private String username;
    private String password;
    private Role role; // Changed from String to Role

    public CustomUserDetails(UserCredential userCredential) {
        this.username = userCredential.getName();
        this.password = userCredential.getPassword();
        this.role = userCredential.getRole(); // Now assigns Role entity
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Extract role name from Role entity
        String roleName = (role != null) ? role.getName() : "USER";
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
    // Getter for role entity if needed elsewhere
    public Role getRole() {
        return role;
    }
    
    // Getter for role name as string
    public String getRoleName() {
        return (role != null) ? role.getName() : "USER";
    }
} 