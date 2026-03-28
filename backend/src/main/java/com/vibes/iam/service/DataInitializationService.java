package com.vibes.iam.service;

import com.vibes.iam.entity.Permission;
import com.vibes.iam.entity.Role;
import com.vibes.iam.entity.User;
import com.vibes.iam.repository.PermissionRepository;
import com.vibes.iam.repository.RoleRepository;
import com.vibes.iam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializePermissions();
        initializeRoles();
        initializeUsers();
    }

    private void initializePermissions() {
        List<Permission> defaultPermissions = Arrays.asList(
            new Permission("READ_USERS", "Read user information", "USER", "READ"),
            new Permission("CREATE_USERS", "Create new users", "USER", "CREATE"),
            new Permission("UPDATE_USERS", "Update user information", "USER", "UPDATE"),
            new Permission("DELETE_USERS", "Delete users", "USER", "DELETE"),
            new Permission("MANAGE_USERS", "Manage user accounts (enable/disable/unlock)", "USER", "MANAGE"),
            
            new Permission("READ_ROLES", "Read role information", "ROLE", "READ"),
            new Permission("CREATE_ROLES", "Create new roles", "ROLE", "CREATE"),
            new Permission("UPDATE_ROLES", "Update role information", "ROLE", "UPDATE"),
            new Permission("DELETE_ROLES", "Delete roles", "ROLE", "DELETE"),
            new Permission("MANAGE_ROLES", "Manage roles (activate/deactivate)", "ROLE", "MANAGE"),
            
            new Permission("READ_PERMISSIONS", "Read permission information", "PERMISSION", "READ"),
            new Permission("CREATE_PERMISSIONS", "Create new permissions", "PERMISSION", "CREATE"),
            new Permission("UPDATE_PERMISSIONS", "Update permission information", "PERMISSION", "UPDATE"),
            new Permission("DELETE_PERMISSIONS", "Delete permissions", "PERMISSION", "DELETE"),
            new Permission("MANAGE_PERMISSIONS", "Manage permissions", "PERMISSION", "MANAGE"),
            
            new Permission("READ_AUDIT_LOGS", "Read audit logs", "AUDIT", "READ"),
            new Permission("MANAGE_SESSIONS", "Manage user sessions", "SESSION", "MANAGE"),
            new Permission("VALIDATE_SESSIONS", "Validate sessions", "SESSION", "VALIDATE")
        );

        for (Permission permission : defaultPermissions) {
            if (!permissionRepository.existsByName(permission.getName())) {
                permissionRepository.save(permission);
            }
        }
    }

    private void initializeRoles() {
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role("ADMIN", "System Administrator");
            Set<Permission> adminPermissions = new HashSet<>(permissionRepository.findAll());
            adminRole.setPermissions(adminPermissions);
            roleRepository.save(adminRole);
        }

        if (!roleRepository.existsByName("USER_MANAGER")) {
            Role userManagerRole = new Role("USER_MANAGER", "User Manager");
            Set<Permission> userManagerPermissions = new HashSet<>();
            userManagerPermissions.addAll(permissionRepository.findByResource("USER"));
            userManagerPermissions.add(permissionRepository.findByName("READ_ROLES").orElse(null));
            userManagerPermissions.add(permissionRepository.findByName("MANAGE_SESSIONS").orElse(null));
            userManagerRole.setPermissions(userManagerPermissions);
            roleRepository.save(userManagerRole);
        }

        if (!roleRepository.existsByName("AUDITOR")) {
            Role auditorRole = new Role("AUDITOR", "System Auditor");
            Set<Permission> auditorPermissions = new HashSet<>();
            auditorPermissions.add(permissionRepository.findByName("READ_USERS").orElse(null));
            auditorPermissions.add(permissionRepository.findByName("READ_ROLES").orElse(null));
            auditorPermissions.add(permissionRepository.findByName("READ_PERMISSIONS").orElse(null));
            auditorPermissions.add(permissionRepository.findByName("READ_AUDIT_LOGS").orElse(null));
            auditorRole.setPermissions(auditorPermissions);
            roleRepository.save(auditorRole);
        }

        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role("USER", "Regular User");
            userRole.setPermissions(new HashSet<>());
            roleRepository.save(userRole);
        }
    }

    private void initializeUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@vibes.com", passwordEncoder.encode("Admin@123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            
            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            if (adminRole != null) {
                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(adminRole);
                admin.setRoles(adminRoles);
            }
            
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("demo")) {
            User demo = new User("demo", "demo@vibes.com", passwordEncoder.encode("Demo@123"));
            demo.setFirstName("Demo");
            demo.setLastName("User");
            
            Role userRole = roleRepository.findByName("USER").orElse(null);
            if (userRole != null) {
                Set<Role> demoRoles = new HashSet<>();
                demoRoles.add(userRole);
                demo.setRoles(demoRoles);
            }
            
            userRepository.save(demo);
        }
    }
}