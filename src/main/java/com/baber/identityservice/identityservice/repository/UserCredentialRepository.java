package com.baber.identityservice.identityservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.baber.identityservice.identityservice.entity.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {
    Optional<UserCredential> findByName(String username);
    @Query(value = "SELECT * FROM user_credential WHERE email = ?1", nativeQuery = true)
    Optional<UserCredential> findByEmail(String email);



}
