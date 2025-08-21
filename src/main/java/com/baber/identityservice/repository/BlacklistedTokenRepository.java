package com.baber.identityservice.repository;

import com.baber.identityservice.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    
    /**
     * Find active blacklisted token by token hash
     */
    Optional<BlacklistedToken> findByTokenHashAndActiveTrue(String tokenHash);
    
    /**
     * Find all active blacklisted tokens
     */
    List<BlacklistedToken> findByActiveTrue();
    
    /**
     * Count active blacklisted tokens
     */
    long countByActiveTrue();
    
    /**
     * Find expired tokens for cleanup
     */
    @Query("SELECT bt FROM BlacklistedToken bt WHERE bt.expirationTime < :now AND bt.active = true")
    List<BlacklistedToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Soft delete by setting active to false
     */
    @Query("UPDATE BlacklistedToken bt SET bt.active = false WHERE bt.tokenHash = :tokenHash")
    void softDeleteByTokenHash(@Param("tokenHash") String tokenHash);
} 