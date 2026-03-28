package com.vibes.iam.controller;

import com.vibes.iam.entity.AuditLog;
import com.vibes.iam.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logging", description = "Audit logging APIs")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_AUDIT_LOGS')")
    @Operation(summary = "Get all audit logs", description = "Retrieve paginated list of all audit logs")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('READ_AUDIT_LOGS')")
    @Operation(summary = "Get user audit logs", description = "Retrieve audit logs for specific user")
    public ResponseEntity<Page<AuditLog>> getUserAuditLogs(@PathVariable Long userId, Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getUserAuditLogs(userId, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAuthority('READ_AUDIT_LOGS')")
    @Operation(summary = "Get audit logs by action", description = "Retrieve audit logs for specific action")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(@PathVariable String action, Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getAuditLogsByAction(action, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAuthority('READ_AUDIT_LOGS')")
    @Operation(summary = "Get audit logs by date range", description = "Retrieve audit logs within date range")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getAuditLogsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/failed")
    @PreAuthorize("hasAuthority('READ_AUDIT_LOGS')")
    @Operation(summary = "Get failed actions", description = "Retrieve audit logs for failed actions")
    public ResponseEntity<Page<AuditLog>> getFailedActions(Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getFailedActions(pageable);
        return ResponseEntity.ok(auditLogs);
    }
}