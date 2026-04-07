package com.baber.identityservice.config;

import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class TracingExclusionConfig {

    @Bean
    ObservationPredicate actuatorExclusionPredicate() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverCtx) {
                Object carrier = serverCtx.getCarrier();
                String path = null;

                if (carrier instanceof HttpServletRequest servletRequest) {
                    path = servletRequest.getRequestURI();
                }

                return path == null || !path.startsWith("/actuator");
            }
            return true;
        };
    }
}


