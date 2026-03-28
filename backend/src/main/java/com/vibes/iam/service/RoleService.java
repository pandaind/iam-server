package com.vibes.iam.service;

import com.vibes.iam.dto.CreateRoleRequest;
import com.vibes.iam.dto.RoleDto;
import com.vibes.iam.entity.Permission;
import com.vibes.iam.entity.Role;
import com.vibes.iam.exception.DuplicateResourceException;
import com.vibes.iam.exception.ResourceNotFoundException;
import com.vibes.iam.repository.PermissionRepository;
import com.vibes.iam.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditService auditService;

    public Page<RoleDto> getAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable)
                .map(RoleDto::new);
    }

    public List<RoleDto> getActiveRoles() {
        return roleRepository.findByActiveTrue().stream()
                .map(RoleDto::new)
                .collect(Collectors.toList());
    }

    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return new RoleDto(role);
    }

    public RoleDto getRoleByName(String name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name));
        return new RoleDto(role);
    }

    public RoleDto createRole(CreateRoleRequest request, String currentUsername) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role already exists: " + request.getName());
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            Set<Permission> permissions = request.getPermissions().stream()
                    .map(permissionName -> permissionRepository.findByName(permissionName)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);

        auditService.logUserAction(null, currentUsername, "CREATE_ROLE", "ROLE", 
                "Created role: " + savedRole.getName(), null, null, true, null);

        return new RoleDto(savedRole);
    }

    public RoleDto updateRole(Long id, CreateRoleRequest request, String currentUsername) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role already exists: " + request.getName());
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissions() != null) {
            Set<Permission> permissions = request.getPermissions().stream()
                    .map(permissionName -> permissionRepository.findByName(permissionName)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);

        auditService.logUserAction(null, currentUsername, "UPDATE_ROLE", "ROLE", 
                "Updated role: " + savedRole.getName(), null, null, true, null);

        return new RoleDto(savedRole);
    }

    public void deleteRole(Long id, String currentUsername) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        roleRepository.delete(role);

        auditService.logUserAction(null, currentUsername, "DELETE_ROLE", "ROLE", 
                "Deleted role: " + role.getName(), null, null, true, null);
    }

    public void activateRole(Long id, String currentUsername) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setActive(true);
        roleRepository.save(role);

        auditService.logUserAction(null, currentUsername, "ACTIVATE_ROLE", "ROLE", 
                "Activated role: " + role.getName(), null, null, true, null);
    }

    public void deactivateRole(Long id, String currentUsername) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setActive(false);
        roleRepository.save(role);

        auditService.logUserAction(null, currentUsername, "DEACTIVATE_ROLE", "ROLE", 
                "Deactivated role: " + role.getName(), null, null, true, null);
    }

    public void assignPermissionToRole(Long roleId, String permissionName, String currentUsername) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName));

        role.addPermission(permission);
        roleRepository.save(role);

        auditService.logUserAction(null, currentUsername, "ASSIGN_PERMISSION", "ROLE", 
                "Assigned permission " + permissionName + " to role " + role.getName(), null, null, true, null);
    }

    public void revokePermissionFromRole(Long roleId, String permissionName, String currentUsername) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName));

        role.removePermission(permission);
        roleRepository.save(role);

        auditService.logUserAction(null, currentUsername, "REVOKE_PERMISSION", "ROLE", 
                "Revoked permission " + permissionName + " from role " + role.getName(), null, null, true, null);
    }
}