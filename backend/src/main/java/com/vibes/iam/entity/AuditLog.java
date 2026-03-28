package com.vibes.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    @Column(nullable = false)
    private String username;

    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action cannot exceed 100 characters")
    @Column(nullable = false)
    private String action;

    @Size(max = 100, message = "Resource cannot exceed 100 characters")
    @Column
    private String resource;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    @Column
    private String ipAddress;

    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    @Column
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean success = true;

    @Size(max = 255, message = "Error message cannot exceed 255 characters")
    @Column
    private String errorMessage;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(Long userId, String username, String action, String resource, String details, 
                   String ipAddress, String userAgent, boolean success, String errorMessage) {
        this();
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.resource = resource;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}