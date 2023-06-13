package com.baber.identityservice.identityservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.baber.identityservice.identityservice.entity.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential,Integer>{

    Optional<UserCredential> findByName(String username);
    
}
