package com.baber.identityservice.identityservice.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.baber.identityservice.identityservice.entity.UserCredential;
import com.baber.identityservice.identityservice.repository.UserCredentialRepository;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    @Autowired
    private UserCredentialRepository repository;

//    @Override
//    public Mono<UserDetails> findByUsername(String username) {
//        return repository.findByName(username)
//                .map(userCredential -> {
//                    UserDetails userDetails = new CustomUserDetails(userCredential);
//                    return userDetails;
//                })
//                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found with name: " + username)));
//    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {

        return repository.findByName(username).switchIfEmpty(Mono.defer(() -> {

            return Mono.error(new UsernameNotFoundException("User Not Found"));

        })).map(UserCredential::toUserDetails);
    }



}
