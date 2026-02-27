package com.baber.identityservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.baber.identityservice.entity.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    @Query(value = "SELECT * FROM user_credential WHERE CONCAT(first_name, ' ', last_name) = ?1", nativeQuery = true)
    Optional<UserCredential> findByName(String fullName);
    
    @Query(value = "SELECT * FROM user_credential WHERE email = ?1", nativeQuery = true)
    Optional<UserCredential> findByEmail(String email);
    
    @Query(value = "SELECT * FROM user_credential WHERE phone = ?1", nativeQuery = true)
    Optional<UserCredential> findByPhone(String phone);

}
