package com.baber.identityservice.identityservice.repository;

import com.baber.identityservice.identityservice.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.name = :roleName AND r.salonId = :salonId")
    Set<Permission> findBySalonAndRole(@Param("salonId") Long salonId, @Param("roleName") String roleName);

    Optional<Permission> findByName(String name);
}

