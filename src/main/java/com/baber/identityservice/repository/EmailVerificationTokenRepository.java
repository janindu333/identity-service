package com.baber.identityservice.repository;

import com.baber.identityservice.entity.EmailVerificationToken;
import com.baber.identityservice.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByTokenAndIsUsedFalse(String token);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.token = :token AND t.isUsed = false AND t.expiresAt > :now")
    Optional<EmailVerificationToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user = :user AND t.isUsed = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    Optional<EmailVerificationToken> findLatestValidTokenByUser(@Param("user") UserCredential user, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EmailVerificationToken t SET t.isUsed = true, t.usedAt = :usedAt WHERE t.user = :user AND t.isUsed = false")
    void invalidateAllTokensForUser(@Param("user") UserCredential user, @Param("usedAt") LocalDateTime usedAt);
}
