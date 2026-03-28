package com.vibes.iam.controller;

import com.vibes.iam.dto.PermissionDto;
import com.vibes.iam.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Permission Management", description = "Permission management APIs")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PERMISSIONS')")
    @Operation(summary = "Get all permissions", description = "Retrieve paginated list of all permissions")
    public ResponseEntity<Page<PermissionDto>> getAllPermissions(Pageable pageable) {
        Page<PermissionDto> permissions = permissionService.getAllPermissions(pageable);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('READ_PERMISSIONS')")
    @Operation(summary = "Get all permissions list", description = "Retrieve complete list of all permissions")
    public ResponseEntity<List<PermissionDto>> getAllPermissionsList() {
        List<PermissionDto> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PERMISSIONS')")
    @Operation(summary = "Get permission by ID", description = "Retrieve permission details by ID")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable Long id) {
        PermissionDto permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(permission);
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAuthority('READ_PERMISSIONS')")
    @Operation(summary = "Get permission by name", description = "Retrieve permission details by name")
    public ResponseEntity<PermissionDto> getPermissionByName(@PathVariable String name) {
        PermissionDto permission = permissionService.getPermissionByName(name);
        return ResponseEntity.ok(permission);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_PERMISSIONS')")
    @Operation(summary = "Create permission", description = "Create a new permission")
    public ResponseEntity<PermissionDto> createPermission(@RequestParam String name,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String resource,
                                                         @RequestParam(required = false) String action,
                                                         Authentication authentication) {
        PermissionDto createdPermission = permissionService.createPermission(name, description, resource, action, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPermission);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_PERMISSIONS')")
    @Operation(summary = "Update permission", description = "Update permission details")
    public ResponseEntity<PermissionDto> updatePermission(@PathVariable Long id,
                                                         @RequestParam String name,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String resource,
                                                         @RequestParam(required = false) String action,
                                                         Authentication authentication) {
        PermissionDto updatedPermission = permissionService.updatePermission(id, name, description, resource, action, authentication.getName());
        return ResponseEntity.ok(updatedPermission);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PERMISSIONS')")
    @Operation(summary = "Delete permission", description = "Delete a permission")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id, Authentication authentication) {
        permissionService.deletePermission(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resource/{resource}")
    @PreAuthorize("hasAuthority('READ_PERMISSIONS')")
    @Operation(summary = "Get permissions by resource", description = "Retrieve all permissions for specific resource")
    public ResponseEntity<List<PermissionDto>> getPermissionsByResource(@PathVariable String resource) {
        List<PermissionDto> permissions = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAuthority('READ_PERMISSIONS')")
    @Operation(summary = "Get permissions by action", description = "Retrieve all permissions for specific action")
    public ResponseEntity<List<PermissionDto>> getPermissionsByAction(@PathVariable String action) {
        List<PermissionDto> permissions = permissionService.getPermissionsByAction(action);
        return ResponseEntity.ok(permissions);
    }
}