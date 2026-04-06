package com.baber.identityservice.identityservice.repository;

import com.baber.identityservice.identityservice.entity.UserCredential;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
public interface UserCredentialRepository extends ReactiveCrudRepository<UserCredential, Integer> {
    Mono<UserCredential> findByName(String username);
    Mono<UserCredential> findByEmail(String email);
    Mono<Void> deleteByName(String name);

}
