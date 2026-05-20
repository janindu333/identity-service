package com.baber.identityservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.baber.identityservice.entity.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM user_credential WHERE keycloak_user_id = ?1", nativeQuery = true)
    Optional<UserCredential> findByKeycloakUserId(String keycloakUserId);

}
