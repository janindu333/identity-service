package com.baber.identityservice.repository;

import com.baber.identityservice.entity.PasswordHistory;
import com.baber.identityservice.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    
    /**
     * Find recent password history for a user (last N passwords)
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.changedAt DESC")
    List<PasswordHistory> findRecentPasswordsByUser(@Param("user") UserCredential user);
    
    /**
     * Check if a password hash exists in user's recent history
     */
    @Query("SELECT COUNT(ph) > 0 FROM PasswordHistory ph WHERE ph.user = :user AND ph.passwordHash = :passwordHash")
    boolean existsByUserAndPasswordHash(@Param("user") UserCredential user, @Param("passwordHash") String passwordHash);
    
    /**
     * Get last N passwords for a user
     */
    @Query(value = "SELECT * FROM password_history WHERE user_id = :userId ORDER BY changed_at DESC LIMIT :limit", nativeQuery = true)
    List<PasswordHistory> findLastNPasswords(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * Cleanup old password history (older than specified days)
     */
    @Modifying
    @Query("DELETE FROM PasswordHistory ph WHERE ph.changedAt < :cutoffDate")
    void deleteOldPasswordHistory(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete all password history for a user
     */
    @Modifying
    @Query("DELETE FROM PasswordHistory ph WHERE ph.user = :user")
    void deleteByUser(@Param("user") UserCredential user);
}

