package com.baber.identityservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Swagger UI triggers CORS preflight (OPTIONS) before calling POST /auth/login.
 * Some Spring Security/CORS combinations in this project reject that preflight
 * with "Invalid CORS request". This filter short-circuits OPTIONS to make
 * preflight succeed, then the real POST request will be handled normally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsPreflightBypassFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only handle CORS preflight.
        return request.getMethod() != null ? !HttpMethod.OPTIONS.matches(request.getMethod()) : true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.VARY, "Origin");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,PATCH,OPTIONS");

        String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isBlank()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        }

        response.setStatus(HttpStatus.OK.value());
        response.getWriter().write("");
    }
}

