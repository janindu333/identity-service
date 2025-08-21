package com.baber.identityservice.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class SystemLoggedInUserAuditorAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String userDetailsJson = UserContext.getUserDetailsJson();

        if (userDetailsJson == null || userDetailsJson.isEmpty()) {
            return Optional.of("system");
        }
        return Optional.of(userDetailsJson);
    }
}
