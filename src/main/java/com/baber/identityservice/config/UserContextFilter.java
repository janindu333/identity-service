package com.baber.identityservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1) // Specify the order of the filter in the filter chain
public class UserContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userAgent = request.getHeader("X-User-Details");
            UserContext.setUserDetailsJson(userAgent);
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear(); // Make sure to clear the user details after the request is processed
        }
    }
}
