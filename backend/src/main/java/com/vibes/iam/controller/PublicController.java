package com.vibes.iam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/public")
@Tag(name = "Public", description = "Public endpoints that don't require authentication")
public class PublicController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is healthy")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "IAM Server",
                "version", "1.0.0"
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "Service info", description = "Get service information")
    public ResponseEntity<?> info() {
        return ResponseEntity.ok(Map.of(
                "name", "IAM Server",
                "description", "Identity and Access Management Server",
                "version", "1.0.0",
                "features", new String[]{
                        "JWT Authentication",
                        "OAuth2 Authorization Server",
                        "Role-Based Access Control",
                        "Session Management",
                        "Password Policies",
                        "Audit Logging",
                        "User Management"
                },
                "documentation", "/swagger-ui.html"
        ));
    }

    @GetMapping("/capabilities")
    @Operation(summary = "Get capabilities", description = "Get server capabilities and supported features")
    public ResponseEntity<?> capabilities() {
        return ResponseEntity.ok(Map.of(
                "authentication", Map.of(
                        "jwt", true,
                        "oauth2", true,
                        "session", true
                ),
                "authorization", Map.of(
                        "rbac", true,
                        "permissions", true,
                        "roles", true
                ),
                "security", Map.of(
                        "passwordPolicies", true,
                        "accountLocking", true,
                        "auditLogging", true,
                        "sessionManagement", true
                ),
                "oauth2Endpoints", Map.of(
                        "authorization", "/oauth2/authorize",
                        "token", "/oauth2/token",
                        "introspect", "/oauth2/introspect",
                        "revoke", "/oauth2/revoke",
                        "jwks", "/.well-known/jwks.json"
                )
        ));
    }
}