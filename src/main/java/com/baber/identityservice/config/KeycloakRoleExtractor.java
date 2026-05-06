package com.baber.identityservice.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class KeycloakRoleExtractor {

    private static final String ROLE_PREFIX = "ROLE_";

    private KeycloakRoleExtractor() {
    }

    static Collection<GrantedAuthority> extract(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Object realmAccessObj = jwt.getClaim("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof Collection<?> roles) {
                addRoles(authorities, roles);
            }
        }

        Object resourceAccessObj = jwt.getClaim("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            for (Object clientObj : resourceAccess.values()) {
                if (clientObj instanceof Map<?, ?> clientAccess) {
                    Object rolesObj = clientAccess.get("roles");
                    if (rolesObj instanceof Collection<?> roles) {
                        addRoles(authorities, roles);
                    }
                }
            }
        }

        return authorities;
    }

    private static void addRoles(List<GrantedAuthority> authorities, Collection<?> roles) {
        for (Object roleObj : roles) {
            if (roleObj == null) {
                continue;
            }
            String role = roleObj.toString().trim();
            if (role.isEmpty()) {
                continue;
            }
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()));
        }
    }
}
