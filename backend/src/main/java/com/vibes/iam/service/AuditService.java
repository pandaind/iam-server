package com.vibes.iam.service;

import com.vibes.iam.entity.AuditLog;
import com.vibes.iam.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    public void logUserAction(Long userId, String username, String action, String resource, 
                             String details, String ipAddress, String userAgent, 
                             boolean success, String errorMessage) {
        AuditLog auditLog = new AuditLog(userId, username, action, resource, details, 
                                        ipAddress, userAgent, success, errorMessage);
        auditLogRepository.save(auditLog);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate, pageable);
    }

    public Page<AuditLog> getFailedActions(Pageable pageable) {
        return auditLogRepository.findFailedActions(pageable);
    }
}