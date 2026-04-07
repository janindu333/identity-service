package com.baber.identityservice.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.baber.identityservice.entity.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.name = :roleName AND p.deleted = 0")
    Set<Permission> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT p FROM Permission p WHERE p.name = :name AND p.deleted = 0")
    Optional<Permission> findByName(@Param("name") String name);
    
    @Query("SELECT DISTINCT p FROM Permission p LEFT JOIN FETCH p.roles WHERE p.deleted = 0")
    List<Permission> findAllActive();
    
    @Modifying
    @Query("UPDATE Permission p SET p.deleted = 1 WHERE p.id = :permissionId")
    void softDeleteById(@Param("permissionId") Long permissionId);
}

