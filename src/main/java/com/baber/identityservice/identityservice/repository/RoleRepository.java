package com.baber.identityservice.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.baber.identityservice.identityservice.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}
