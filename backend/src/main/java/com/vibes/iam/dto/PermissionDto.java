package com.vibes.iam.dto;

import com.vibes.iam.entity.Permission;

import java.time.LocalDateTime;

public class PermissionDto {

    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PermissionDto() {}

    public PermissionDto(Permission permission) {
        this.id = permission.getId();
        this.name = permission.getName();
        this.description = permission.getDescription();
        this.resource = permission.getResource();
        this.action = permission.getAction();
        this.createdAt = permission.getCreatedAt();
        this.updatedAt = permission.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}