package com.baber.identityservice.controller;

import com.baber.identityservice.dto.TokenBlacklistRequest;
import com.baber.identityservice.dto.TokenRemoveRequest;
import com.baber.identityservice.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/admin/tokens")
@Tag(name = "Token Blacklist", description = "Token blacklist management endpoints")
// @PreAuthorize("hasRole('ADMIN')") // Removed for public access
public class TokenBlacklistController {
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    /**
     * Blacklist a token (Admin only)
     */
    @PostMapping("/blacklist")
    @Operation(
        summary = "Blacklist a token",
        description = "Add a JWT token to the blacklist to invalidate it",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token blacklisted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - missing required fields")
        }
    )
    public ResponseEntity<Map<String, Object>> blacklistToken(@RequestBody TokenBlacklistRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        tokenBlacklistService.blacklistToken(
            request.getToken(), 
            request.getUsername(), 
            request.getReason(), 
            request.getExpirationHours() != null ? request.getExpirationHours() : 2
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Token blacklisted successfully");
        response.put("blacklistCount", tokenBlacklistService.getBlacklistStats().getTotalCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Remove a token from blacklist (Admin only)
     */
    @DeleteMapping("/blacklist")
    @Operation(
        summary = "Remove token from blacklist",
        description = "Remove a JWT token from the blacklist",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token removed from blacklist successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - missing token")
        }
    )
    public ResponseEntity<Map<String, Object>> removeFromBlacklist(@RequestBody TokenRemoveRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        tokenBlacklistService.removeFromBlacklist(request.getToken());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Token removed from blacklist");
        response.put("blacklistCount", tokenBlacklistService.getBlacklistStats().getTotalCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if a token is blacklisted
     */
    @GetMapping("/check")
    @Operation(
        summary = "Check if token is blacklisted",
        description = "Check if a JWT token is currently blacklisted",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token blacklist status retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - missing token parameter")
        }
    )
    public ResponseEntity<Map<String, Object>> checkToken(@RequestParam String token) {
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isBlacklisted", isBlacklisted);
        response.put("blacklistCount", tokenBlacklistService.getBlacklistStats().getTotalCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get blacklist statistics (Admin only)
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get blacklist statistics",
        description = "Get current statistics about blacklisted tokens",
        responses = {
            @ApiResponse(responseCode = "200", description = "Blacklist statistics retrieved successfully")
        }
    )
    public ResponseEntity<Map<String, Object>> getStats() {
        TokenBlacklistService.BlacklistStats stats = tokenBlacklistService.getBlacklistStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("redisCount", stats.getRedisCount());
        response.put("databaseCount", stats.getDatabaseCount());
        response.put("totalCount", stats.getTotalCount());
        response.put("message", "Blacklist statistics retrieved");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Rebuild Redis cache from MySQL database (Admin only)
     */
    @PostMapping("/rebuild-cache")
    @Operation(
        summary = "Rebuild Redis cache from database",
        description = "Manually rebuild Redis cache from MySQL database (useful after Redis restart)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cache rebuilt successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to rebuild cache")
        }
    )
    public ResponseEntity<Map<String, Object>> rebuildCache() {
        try {
            tokenBlacklistService.rebuildCacheFromDatabase();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Redis cache rebuilt from database successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to rebuild cache: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Health check for blacklist system
     */
    @GetMapping("/health")
    @Operation(
        summary = "Blacklist system health check",
        description = "Check the health status of the blacklist system (Redis + MySQL)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Health check completed"),
            @ApiResponse(responseCode = "503", description = "System unhealthy")
        }
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            TokenBlacklistService.BlacklistStats stats = tokenBlacklistService.getBlacklistStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", "HEALTHY");
            response.put("redisCount", stats.getRedisCount());
            response.put("databaseCount", stats.getDatabaseCount());
            response.put("totalCount", stats.getTotalCount());
            response.put("message", "Blacklist system is healthy");
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("status", "UNHEALTHY");
            response.put("message", "Blacklist system health check failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(503).body(response);
        }
    }
} 