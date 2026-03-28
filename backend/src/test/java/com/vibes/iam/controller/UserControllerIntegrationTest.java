package com.vibes.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibes.iam.dto.CreateUserRequest;
import com.vibes.iam.dto.UpdateUserRequest;
import com.vibes.iam.entity.Permission;
import com.vibes.iam.entity.Role;
import com.vibes.iam.entity.User;
import com.vibes.iam.repository.PermissionRepository;
import com.vibes.iam.repository.RoleRepository;
import com.vibes.iam.repository.UserRepository;
import com.vibes.iam.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clean up users only (roles and permissions are created by DataInitializationService)
        userRepository.deleteAll();

        // Find existing roles and permissions created by DataInitializationService
        adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("USER role not found"));
        
        readUsersPermission = permissionRepository.findByName("READ_USERS")
                .orElseThrow(() -> new RuntimeException("READ_USERS permission not found"));
        createUsersPermission = permissionRepository.findByName("CREATE_USERS")
                .orElseThrow(() -> new RuntimeException("CREATE_USERS permission not found"));

        // Note: Permissions and roles are already created by DataInitializationService
        
        // Find or create admin user for testing
        adminUser = userRepository.findByUsername("admin").orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@vibes.com");
            adminUser.setPassword(passwordEncoder.encode("Admin@123"));
            adminUser.setFirstName("System");
            adminUser.setLastName("Administrator");
            adminUser.setEnabled(true);
            adminUser.setAccountNonLocked(true);
            adminUser.setAccountNonExpired(true);
            adminUser.setCredentialsNonExpired(true);
            adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
            userRepository.save(adminUser);
        }

        // Create test regular user
        regularUser = new User();
        regularUser.setUsername("testuser");
        regularUser.setEmail("testuser@example.com");
        regularUser.setPassword(passwordEncoder.encode("UserPass@123"));
        regularUser.setFirstName("Test");
        regularUser.setLastName("User");
        regularUser.setEnabled(true);
        regularUser.setAccountNonLocked(true);
        regularUser.setAccountNonExpired(true);
        regularUser.setCredentialsNonExpired(true);
        regularUser.setRoles(new HashSet<>(Set.of(userRole)));
        userRepository.save(regularUser);

        // Generate admin token with all user management permissions
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("READ_USERS"),
                new SimpleGrantedAuthority("CREATE_USERS"),
                new SimpleGrantedAuthority("UPDATE_USERS"),
                new SimpleGrantedAuthority("DELETE_USERS"),
                new SimpleGrantedAuthority("MANAGE_USERS")
        );
        adminToken = jwtUtil.generateToken("admin", adminUser.getId(), authorities);
    }

    @Test
    void getAllUsers_ShouldReturnUsers_WhenAuthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllUsers_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenAuthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/" + adminUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@vibes.com"))
                .andExpect(jsonPath("$.firstName").value("System"));
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/99999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_ShouldCreateUser_WhenValidRequest() throws Exception {
        // Given
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setPassword("NewUser@123");
        createUserRequest.setFirstName("New");
        createUserRequest.setLastName("User");
        createUserRequest.setRoles(new HashSet<>(Set.of("USER")));

        // When & Then
        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenUsernameExists() throws Exception {
        // Given
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("admin"); // Already exists
        createUserRequest.setEmail("another@example.com");
        createUserRequest.setPassword("NewUser@123");
        createUserRequest.setFirstName("Another");
        createUserRequest.setLastName("User");
        createUserRequest.setRoles(new HashSet<>(Set.of("USER")));

        // When & Then
        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: admin"));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailExists() throws Exception {
        // Given
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setEmail("admin@vibes.com"); // Already exists
        createUserRequest.setPassword("NewUser@123");
        createUserRequest.setFirstName("New");
        createUserRequest.setLastName("User");
        createUserRequest.setRoles(new HashSet<>(Set.of("USER")));

        // When & Then
        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: admin@vibes.com"));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        // Given
        CreateUserRequest createUserRequest = new CreateUserRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenValidRequest() throws Exception {
        // Given
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUsername("admin");
        updateRequest.setEmail("admin.updated@example.com");
        updateRequest.setFirstName("Updated Admin");
        updateRequest.setLastName("User");
        updateRequest.setRoles(new HashSet<>(Set.of("ADMIN")));
        // Password is intentionally not set (optional for updates)

        // When & Then
        mockMvc.perform(put("/users/" + adminUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin.updated@example.com"))
                .andExpect(jsonPath("$.firstName").value("Updated Admin"));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenAuthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void enableUser_ShouldEnableUser_WhenAuthorized() throws Exception {
        // Given
        regularUser.setEnabled(false);
        userRepository.save(regularUser);

        // When & Then
        mockMvc.perform(post("/users/" + regularUser.getId() + "/enable")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void disableUser_ShouldDisableUser_WhenAuthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/users/" + regularUser.getId() + "/disable")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void unlockUser_ShouldUnlockUser_WhenAuthorized() throws Exception {
        // Given
        regularUser.setAccountNonLocked(false);
        userRepository.save(regularUser);

        // When & Then
        mockMvc.perform(post("/users/" + regularUser.getId() + "/unlock")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getUsersByRole_ShouldReturnUsers_WhenAuthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/role/ADMIN")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("admin"));
    }
}