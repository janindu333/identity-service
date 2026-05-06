package com.baber.identityservice.config;

import com.baber.identityservice.entity.Role;
import com.baber.identityservice.repository.RoleRepository;
import com.baber.identityservice.service.KeycloakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Mirrors Keycloak realm roles into the local DB so DB roles are only used as a permission-mapping layer.
 * Keycloak remains the source of truth for role assignment.
 */
@Component
@Order(1) // After DB init, before permission seeding and app startup
public class KeycloakRoleSync implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakRoleSync.class);

    private final KeycloakService keycloakService;
    private final RoleRepository roleRepository;

    public KeycloakRoleSync(KeycloakService keycloakService, RoleRepository roleRepository) {
        this.keycloakService = keycloakService;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        syncWithRetry("startup");
    }

    @Scheduled(initialDelayString = "${app.rbac.role-sync.initial-delay-ms:60000}", fixedDelayString = "${app.rbac.role-sync.fixed-delay-ms:300000}")
    public void scheduledSync() {
        syncWithRetry("scheduled");
    }

    private void syncWithRetry(String trigger) {
        int maxAttempts = 3;
        long delayMs = 2500L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                int created = syncOnce();
                logger.info("Keycloak role sync succeeded (trigger={}, attempt={}/{}). createdDbRoles={}", trigger, attempt, maxAttempts, created);
                return;
            } catch (Exception e) {
                logger.warn("Keycloak role sync failed (trigger={}, attempt={}/{}): {}", trigger, attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    logger.error("Keycloak role sync exhausted retries (trigger={}).", trigger);
                    return;
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Transactional
    protected int syncOnce() {
        List<String> roleNames = keycloakService.listRealmRoleNames();
        if (roleNames.isEmpty()) {
            throw new IllegalStateException("Keycloak returned no roles");
        }

        Set<String> filtered = new HashSet<>();
        for (String name : roleNames) {
            if (shouldSkipRole(name)) {
                continue;
            }
            filtered.add(name.trim().toLowerCase(Locale.ROOT));
        }
        if (filtered.isEmpty()) {
            throw new IllegalStateException("Keycloak roles filtered to empty set");
        }

        int created = 0;
        for (String name : filtered) {
            if (roleRepository.findByName(name).isPresent()) {
                continue;
            }
            Role role = new Role();
            role.setName(name);
            role.setDescription("Mirrored from Keycloak realm role");
            roleRepository.save(role);
            created++;
            logger.info("Created DB role mirrored from Keycloak: {}", name);
        }
        return created;
    }

    private boolean shouldSkipRole(String name) {
        if (name == null) return true;
        String n = name.trim();
        if (n.isBlank()) return true;
        // Keycloak built-in realm roles that are usually not part of app RBAC
        if ("offline_access".equalsIgnoreCase(n)) return true;
        if ("uma_authorization".equalsIgnoreCase(n)) return true;
        if (n.toLowerCase().startsWith("default-roles-")) return true;
        return false;
    }
}

