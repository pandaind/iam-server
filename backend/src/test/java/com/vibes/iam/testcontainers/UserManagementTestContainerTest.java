package com.vibes.iam.testcontainers;

import com.vibes.iam.dto.CreateUserRequest;
import com.vibes.iam.entity.Permission;
import com.vibes.iam.entity.Role;
import com.vibes.iam.entity.User;
import com.vibes.iam.repository.PermissionRepository;
import com.vibes.iam.repository.RoleRepository;
import com.vibes.iam.repository.UserRepository;
import com.vibes.iam.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User Management using TestContainers.
 * Tests user CRUD operations with real PostgreSQL database.
 */
// @Disabled("TestContainer servlet context configuration issues - core
// functionality tested by UserControllerIntegrationTest")
public class UserManagementTestContainerTest extends BaseIntegrationTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PermissionRepository permissionRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtUtil jwtUtil;

        private User adminUser;
        private User regularUser;
        private Role adminRole;
        private Role userRole;
        private Permission readUsersPermission;
        private Permission createUsersPermission;
        private Permission updateUsersPermission;
        private Permission deleteUsersPermission;
        private String adminToken;

        @BeforeEach
        void setUp() {
                // Clean up users only (roles and permissions are created by
                // DataInitializationService)
                userRepository.deleteAll();

                // Find existing permissions created by DataInitializationService
                readUsersPermission = permissionRepository.findByName("READ_USERS")
                                .orElseThrow(() -> new RuntimeException("READ_USERS permission not found"));

                createUsersPermission = permissionRepository.findByName("CREATE_USERS")
                                .orElseThrow(() -> new RuntimeException("CREATE_USERS permission not found"));

                updateUsersPermission = permissionRepository.findByName("UPDATE_USERS")
                                .orElseThrow(() -> new RuntimeException("UPDATE_USERS permission not found"));

                deleteUsersPermission = permissionRepository.findByName("DELETE_USERS")
                                .orElseThrow(() -> new RuntimeException("DELETE_USERS permission not found"));

                // Find existing roles created by DataInitializationService
                adminRole = roleRepository.findByName("ADMIN")
                                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

                userRole = roleRepository.findByName("USER")
                                .orElseThrow(() -> new RuntimeException("USER role not found"));

                // Create admin user
                adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setEmail("admin@example.com");
                adminUser.setPassword(passwordEncoder.encode("AdminPass@123"));
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setEnabled(true);
                adminUser.setAccountNonLocked(true);
                adminUser.setAccountNonExpired(true);
                adminUser.setCredentialsNonExpired(true);
                adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
                userRepository.save(adminUser);

                // Create regular user
                regularUser = new User();
                regularUser.setUsername("user");
                regularUser.setEmail("user@example.com");
                regularUser.setPassword(passwordEncoder.encode("UserPass@123"));
                regularUser.setFirstName("Regular");
                regularUser.setLastName("User");
                regularUser.setEnabled(true);
                regularUser.setAccountNonLocked(true);
                regularUser.setAccountNonExpired(true);
                regularUser.setCredentialsNonExpired(true);
                regularUser.setRoles(new HashSet<>(Set.of(userRole)));
                userRepository.save(regularUser);

                // Generate admin token
                List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("READ_USERS"),
                                new SimpleGrantedAuthority("CREATE_USERS"),
                                new SimpleGrantedAuthority("UPDATE_USERS"),
                                new SimpleGrantedAuthority("DELETE_USERS"));
                adminToken = jwtUtil.generateToken("admin", adminUser.getId(), authorities);
        }

        @Test
        void getAllUsers_ShouldReturnUsersFromDatabase() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/v1/users")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.content[?(@.username == 'admin')]").exists())
                                .andExpect(jsonPath("$.content[?(@.username == 'user')]").exists());
        }

        @Test
        void createUser_ShouldPersistInDatabase() throws Exception {
                // Given
                CreateUserRequest createUserRequest = new CreateUserRequest();
                createUserRequest.setUsername("newuser");
                createUserRequest.setEmail("newuser@example.com");
                createUserRequest.setPassword("NewUser@123");
                createUserRequest.setFirstName("New");
                createUserRequest.setLastName("User");
                createUserRequest.setRoles(Set.of("USER"));

                // When
                mockMvc.perform(post("/api/v1/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createUserRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.username").value("newuser"))
                                .andExpect(jsonPath("$.email").value("newuser@example.com"));

                // Then - Verify user exists in database
                User createdUser = userRepository.findByUsername("newuser").orElse(null);
                assert createdUser != null;
                assert createdUser.getEmail().equals("newuser@example.com");
                assert createdUser.getFirstName().equals("New");
                assert createdUser.getLastName().equals("User");
                assert passwordEncoder.matches("NewUser@123", createdUser.getPassword());
                assert createdUser.getRoles().stream().anyMatch(role -> role.getName().equals("USER"));
        }

        @Test
        void updateUser_ShouldUpdateInDatabase() throws Exception {
                // Given
                CreateUserRequest updateRequest = new CreateUserRequest();
                updateRequest.setUsername("user");
                updateRequest.setEmail("user.updated@example.com");
                updateRequest.setFirstName("Updated Regular");
                updateRequest.setLastName("User");
                updateRequest.setRoles(Set.of("USER"));

                // When
                mockMvc.perform(put("/api/v1/users/" + regularUser.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("user.updated@example.com"))
                                .andExpect(jsonPath("$.firstName").value("Updated Regular"));

                // Then - Verify user is updated in database
                User updatedUser = userRepository.findById(regularUser.getId()).orElse(null);
                assert updatedUser != null;
                assert updatedUser.getEmail().equals("user.updated@example.com");
                assert updatedUser.getFirstName().equals("Updated Regular");
        }

        @Test
        void deleteUser_ShouldRemoveFromDatabase() throws Exception {
                // When
                mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isNoContent());

                // Then - Verify user is deleted from database
                boolean userExists = userRepository.existsById(regularUser.getId());
                assert !userExists;
        }

        @Test
        void enableUser_ShouldUpdateStatusInDatabase() throws Exception {
                // Given - Disable user first
                regularUser.setEnabled(false);
                userRepository.save(regularUser);

                // When
                mockMvc.perform(post("/api/v1/users/" + regularUser.getId() + "/enable")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());

                // Then - Verify user is enabled in database
                User enabledUser = userRepository.findById(regularUser.getId()).orElse(null);
                assert enabledUser != null;
                assert enabledUser.isEnabled();
        }

        @Test
        void disableUser_ShouldUpdateStatusInDatabase() throws Exception {
                // When
                mockMvc.perform(post("/api/v1/users/" + regularUser.getId() + "/disable")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());

                // Then - Verify user is disabled in database
                User disabledUser = userRepository.findById(regularUser.getId()).orElse(null);
                assert disabledUser != null;
                assert !disabledUser.isEnabled();
        }

        @Test
        void unlockUser_ShouldUpdateLockStatusInDatabase() throws Exception {
                // Given - Lock user first
                regularUser.setAccountNonLocked(false);
                regularUser.setFailedLoginAttempts(5);
                userRepository.save(regularUser);

                // When
                mockMvc.perform(post("/api/v1/users/" + regularUser.getId() + "/unlock")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());

                // Then - Verify user is unlocked in database
                User unlockedUser = userRepository.findById(regularUser.getId()).orElse(null);
                assert unlockedUser != null;
                assert unlockedUser.isAccountNonLocked();
                assert unlockedUser.getFailedLoginAttempts() == 0;
                assert unlockedUser.getLockedAt() == null;
        }

        @Test
        void getUsersByRole_ShouldReturnCorrectUsersFromDatabase() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/v1/users/role/ADMIN")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].username").value("admin"));
        }

        @Test
        void createUser_ShouldFailForDuplicateUsername_WithDatabaseConstraint() throws Exception {
                // Given - Try to create user with existing username
                CreateUserRequest createUserRequest = new CreateUserRequest();
                createUserRequest.setUsername("admin"); // Already exists
                createUserRequest.setEmail("another@example.com");
                createUserRequest.setPassword("NewUser@123");
                createUserRequest.setFirstName("Another");
                createUserRequest.setLastName("User");
                createUserRequest.setRoles(Set.of("USER"));

                // When & Then
                mockMvc.perform(post("/api/v1/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createUserRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Username already exists: admin"));

                // Verify only original admin user exists
                List<User> adminUsers = userRepository.findByRoleName("ADMIN");
                assert adminUsers.size() == 1;
                assert adminUsers.get(0).getEmail().equals("admin@example.com");
        }

        @Test
        void getUserById_ShouldReturnUserFromDatabase() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/v1/users/" + adminUser.getId())
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(adminUser.getId()))
                                .andExpect(jsonPath("$.username").value("admin"))
                                .andExpect(jsonPath("$.email").value("admin@example.com"))
                                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        }

        @Test
        void createUser_ShouldHandlePasswordEncryption() throws Exception {
                // Given
                CreateUserRequest createUserRequest = new CreateUserRequest();
                createUserRequest.setUsername("testencryption");
                createUserRequest.setEmail("testencryption@example.com");
                createUserRequest.setPassword("PlainPassword@123");
                createUserRequest.setFirstName("Test");
                createUserRequest.setLastName("Encryption");
                createUserRequest.setRoles(Set.of("USER"));

                // When
                mockMvc.perform(post("/api/v1/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(createUserRequest)))
                                .andExpect(status().isCreated());

                // Then - Verify password is encrypted in database
                User createdUser = userRepository.findByUsername("testencryption").orElse(null);
                assert createdUser != null;
                assert !createdUser.getPassword().equals("PlainPassword@123"); // Password should be encrypted
                assert passwordEncoder.matches("PlainPassword@123", createdUser.getPassword()); // But should match when
                                                                                                // checked
        }
}