package com.vibes.iam.controller;

import com.vibes.iam.dto.CreateRoleRequest;
import com.vibes.iam.dto.RoleDto;
import com.vibes.iam.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/roles")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Role Management", description = "Role management APIs")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_ROLES')")
    @Operation(summary = "Get all roles", description = "Retrieve paginated list of all roles")
    public ResponseEntity<Page<RoleDto>> getAllRoles(Pageable pageable) {
        Page<RoleDto> roles = roleService.getAllRoles(pageable);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('READ_ROLES')")
    @Operation(summary = "Get active roles", description = "Retrieve all active roles")
    public ResponseEntity<List<RoleDto>> getActiveRoles() {
        List<RoleDto> roles = roleService.getActiveRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_ROLES')")
    @Operation(summary = "Get role by ID", description = "Retrieve role details by ID")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        RoleDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAuthority('READ_ROLES')")
    @Operation(summary = "Get role by name", description = "Retrieve role details by name")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable String name) {
        RoleDto role = roleService.getRoleByName(name);
        return ResponseEntity.ok(role);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ROLES')")
    @Operation(summary = "Create role", description = "Create a new role")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request,
                                            Authentication authentication) {
        RoleDto createdRole = roleService.createRole(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_ROLES')")
    @Operation(summary = "Update role", description = "Update role details")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id,
                                            @Valid @RequestBody CreateRoleRequest request,
                                            Authentication authentication) {
        RoleDto updatedRole = roleService.updateRole(id, request, authentication.getName());
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_ROLES')")
    @Operation(summary = "Delete role", description = "Delete a role")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id, Authentication authentication) {
        roleService.deleteRole(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @Operation(summary = "Activate role", description = "Activate a role")
    public ResponseEntity<Void> activateRole(@PathVariable Long id, Authentication authentication) {
        roleService.activateRole(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @Operation(summary = "Deactivate role", description = "Deactivate a role")
    public ResponseEntity<Void> deactivateRole(@PathVariable Long id, Authentication authentication) {
        roleService.deactivateRole(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/permissions/{permissionName}")
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    @Operation(summary = "Assign permission to role", description = "Assign a permission to a role")
    public ResponseEntity<Void> assignPermissionToRole(@PathVariable Long id,
                                                      @PathVariable String permissionName,
                                                      Authentication authentication) {
        roleService.assignPermissionToRole(id, permissionName, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/permissions/{permissionName}")
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    @Operation(summary = "Revoke permission from role", description = "Revoke a permission from a role")
    public ResponseEntity<Void> revokePermissionFromRole(@PathVariable Long id,
                                                        @PathVariable String permissionName,
                                                        Authentication authentication) {
        roleService.revokePermissionFromRole(id, permissionName, authentication.getName());
        return ResponseEntity.ok().build();
    }
}