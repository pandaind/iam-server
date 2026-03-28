package com.vibes.iam.controller;

import com.vibes.iam.dto.UserSessionDto;
import com.vibes.iam.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Session Management", description = "Session management APIs")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping("/my")
    @Operation(summary = "Get my active sessions", description = "Retrieve current user's active sessions")
    public ResponseEntity<List<UserSessionDto>> getMyActiveSessions(Authentication authentication) {
        List<UserSessionDto> sessions = sessionService.getUserActiveSessions(authentication.getName());
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/my/{sessionId}")
    @Operation(summary = "Invalidate my session", description = "Invalidate a specific session of current user")
    public ResponseEntity<Void> invalidateMySession(@PathVariable String sessionId, 
                                                   Authentication authentication) {
        sessionService.invalidateUserSession(authentication.getName(), sessionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/my/all")
    @Operation(summary = "Invalidate all my sessions", description = "Invalidate all sessions of current user")
    public ResponseEntity<Void> invalidateAllMySessions(Authentication authentication) {
        sessionService.invalidateAllUserSessions(authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{username}")
    @PreAuthorize("hasAuthority('MANAGE_SESSIONS')")
    @Operation(summary = "Get user sessions", description = "Retrieve active sessions for specific user")
    public ResponseEntity<List<UserSessionDto>> getUserActiveSessions(@PathVariable String username) {
        List<UserSessionDto> sessions = sessionService.getUserActiveSessions(username);
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/user/{username}/{sessionId}")
    @PreAuthorize("hasAuthority('MANAGE_SESSIONS')")
    @Operation(summary = "Invalidate user session", description = "Invalidate a specific session of a user")
    public ResponseEntity<Void> invalidateUserSession(@PathVariable String username,
                                                     @PathVariable String sessionId) {
        sessionService.invalidateUserSession(username, sessionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{username}/all")
    @PreAuthorize("hasAuthority('MANAGE_SESSIONS')")
    @Operation(summary = "Invalidate all user sessions", description = "Invalidate all sessions of a specific user")
    public ResponseEntity<Void> invalidateAllUserSessions(@PathVariable String username) {
        sessionService.invalidateAllUserSessions(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate/{sessionId}")
    @PreAuthorize("hasAuthority('VALIDATE_SESSIONS')")
    @Operation(summary = "Validate session", description = "Check if a session is valid")
    public ResponseEntity<Boolean> validateSession(@PathVariable String sessionId) {
        boolean isValid = sessionService.isSessionValid(sessionId);
        return ResponseEntity.ok(isValid);
    }
}