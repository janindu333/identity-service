package com.baber.identityservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BlacklistedToken {
    
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    
    @Column(name = "token", length = 1000, nullable = false)
    private String token;
    
    @Column(name = "username", length = 100)
    private String username;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;
    
    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "active", nullable = false)
    private boolean active = true;
    
    /**
     * Generate SHA-256 hash of the token for secure storage
     */
    public static String generateTokenHash(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token hash", e);
        }
    }
} 