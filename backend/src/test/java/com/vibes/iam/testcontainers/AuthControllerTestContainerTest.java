package com.vibes.iam.testcontainers;

import com.vibes.iam.dto.LoginRequest;
import com.vibes.iam.entity.Role;
import com.vibes.iam.entity.User;
import com.vibes.iam.repository.RoleRepository;
import com.vibes.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController using TestContainers.
 * Tests the complete authentication flow with real database and Redis
 * containers.
 * 
 * DISABLED due to servlet context configuration issues with TestContainers.
 * Core authentication functionality is fully tested by
 * AuthControllerIntegrationTest.
 */
// @Disabled("TestContainer servlet context configuration issues - core
// functionality tested by AuthControllerIntegrationTest")
public class AuthControllerTestContainerTest extends BaseIntegrationTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private User testUser;
        private Role userRole;

        @BeforeEach
        void setUp() {
                // Clean up users only (roles are created by DataInitializationService)
                userRepository.deleteAll();

                // Find existing role created by DataInitializationService
                userRole = roleRepository.findByName("USER")
                                .orElseThrow(() -> new RuntimeException("USER role not found"));

                // Create test user
                testUser = new User();
                testUser.setUsername("testuser");
                testUser.setEmail("test@example.com");
                testUser.setPassword(passwordEncoder.encode("TestPass@123"));
                testUser.setFirstName("Test");
                testUser.setLastName("User");
                testUser.setEnabled(true);
                testUser.setAccountNonLocked(true);
                testUser.setAccountNonExpired(true);
                testUser.setCredentialsNonExpired(true);
                testUser.setRoles(new HashSet<>(Set.of(userRole)));
                userRepository.save(testUser);
        }

        @Test
        void login_ShouldReturnTokens_WhenValidCredentials() throws Exception {
                // Given
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("TestPass@123");

                // When & Then
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.refreshToken").exists())
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.expiresIn").exists())
                                .andExpect(jsonPath("$.user.username").value("testuser"))
                                .andExpect(jsonPath("$.user.email").value("test@example.com"));
        }

        @Test
        void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
                // Given
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("wrongpassword");

                // When & Then
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        void login_ShouldReturnBadRequest_WhenMissingRequiredFields() throws Exception {
                // Given
                LoginRequest loginRequest = new LoginRequest();
                // Missing username and password

                // When & Then
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors").exists())
                                .andExpect(jsonPath("$.errors.username").exists())
                                .andExpect(jsonPath("$.errors.password").exists());
        }

        @Test
        void login_ShouldLockAccount_WhenMultipleFailedAttempts() throws Exception {
                // Given
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("wrongpassword");

                // When - Attempt failed login 5 times
                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(asJsonString(loginRequest)))
                                        .andExpect(status().isUnauthorized());
                }

                // Then - Account should be locked, even with correct password
                LoginRequest correctLogin = new LoginRequest();
                correctLogin.setUsername("testuser");
                correctLogin.setPassword("TestPass@123");

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(correctLogin)))
                                .andExpect(status().isUnauthorized());

                // Verify account is locked in database
                User lockedUser = userRepository.findByUsername("testuser").orElse(null);
                assert lockedUser != null;
                assert !lockedUser.isAccountNonLocked();
                assert lockedUser.getFailedLoginAttempts() >= 5;
        }

        @Test
        void refreshToken_ShouldReturnNewTokens_WhenValidRefreshToken() throws Exception {
                // Given - First login to get tokens
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("TestPass@123");

                String loginResponse = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                var authResponse = objectMapper.readTree(loginResponse);
                String refreshToken = authResponse.get("refreshToken").asText();

                // When & Then
                mockMvc.perform(post("/auth/refresh")
                                .param("refreshToken", refreshToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.refreshToken").exists())
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.user.username").value("testuser"));
        }

        @Test
        void refreshToken_ShouldReturnUnauthorized_WhenInvalidRefreshToken() throws Exception {
                // When & Then
                mockMvc.perform(post("/auth/refresh")
                                .param("refreshToken", "invalid-refresh-token"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_ShouldReturnOk_WhenValidToken() throws Exception {
                // Given - First login to get token
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("TestPass@123");

                String loginResponse = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                var authResponse = objectMapper.readTree(loginResponse);
                String accessToken = authResponse.get("accessToken").asText();

                // When & Then
                mockMvc.perform(post("/auth/logout")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk());
        }

        @Test
        void login_ShouldPersistLoginAttempts_InDatabase() throws Exception {
                // Given
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("TestPass@123");

                // When - Successful login
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isOk());

                // Then - Verify user's last login is updated in database
                User updatedUser = userRepository.findByUsername("testuser").orElse(null);
                assert updatedUser != null;
                assert updatedUser.getLastLoginAt() != null;
                assert updatedUser.getFailedLoginAttempts() == 0;
        }

        @Test
        void login_ShouldHandleDisabledUser_FromDatabase() throws Exception {
                // Given - Disable user in database
                testUser.setEnabled(false);
                userRepository.save(testUser);

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("testuser");
                loginRequest.setPassword("TestPass@123");

                // When & Then
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(loginRequest)))
                                .andExpect(status().isUnauthorized());
        }
}