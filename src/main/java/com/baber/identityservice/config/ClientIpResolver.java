package com.baber.identityservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }

        header = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }

        return request.getRemoteAddr();
    }
}

