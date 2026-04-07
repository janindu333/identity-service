package com.baber.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.baber.identityservice.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findById(Long id);
    

    Optional<Role> findByName(String name);

}
