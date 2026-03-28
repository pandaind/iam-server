package com.vibes.iam.service;

import com.vibes.iam.dto.PermissionDto;
import com.vibes.iam.entity.Permission;
import com.vibes.iam.exception.DuplicateResourceException;
import com.vibes.iam.exception.ResourceNotFoundException;
import com.vibes.iam.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditService auditService;

    public Page<PermissionDto> getAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable)
                .map(PermissionDto::new);
    }

    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionDto::new)
                .collect(Collectors.toList());
    }

    public PermissionDto getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));
        return new PermissionDto(permission);
    }

    public PermissionDto getPermissionByName(String name) {
        Permission permission = permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with name: " + name));
        return new PermissionDto(permission);
    }

    public PermissionDto createPermission(String name, String description, String resource, String action, String currentUsername) {
        if (permissionRepository.existsByName(name)) {
            throw new DuplicateResourceException("Permission already exists: " + name);
        }

        Permission permission = new Permission(name, description, resource, action);
        Permission savedPermission = permissionRepository.save(permission);

        auditService.logUserAction(null, currentUsername, "CREATE_PERMISSION", "PERMISSION", 
                "Created permission: " + savedPermission.getName(), null, null, true, null);

        return new PermissionDto(savedPermission);
    }

    public PermissionDto updatePermission(Long id, String name, String description, String resource, String action, String currentUsername) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        if (!permission.getName().equals(name) && permissionRepository.existsByName(name)) {
            throw new DuplicateResourceException("Permission already exists: " + name);
        }

        permission.setName(name);
        permission.setDescription(description);
        permission.setResource(resource);
        permission.setAction(action);

        Permission savedPermission = permissionRepository.save(permission);

        auditService.logUserAction(null, currentUsername, "UPDATE_PERMISSION", "PERMISSION", 
                "Updated permission: " + savedPermission.getName(), null, null, true, null);

        return new PermissionDto(savedPermission);
    }

    public void deletePermission(Long id, String currentUsername) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        permissionRepository.delete(permission);

        auditService.logUserAction(null, currentUsername, "DELETE_PERMISSION", "PERMISSION", 
                "Deleted permission: " + permission.getName(), null, null, true, null);
    }

    public List<PermissionDto> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource).stream()
                .map(PermissionDto::new)
                .collect(Collectors.toList());
    }

    public List<PermissionDto> getPermissionsByAction(String action) {
        return permissionRepository.findByAction(action).stream()
                .map(PermissionDto::new)
                .collect(Collectors.toList());
    }
}