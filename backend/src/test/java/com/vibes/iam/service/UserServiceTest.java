package com.vibes.iam.service;

import com.vibes.iam.dto.CreateUserRequest;
import com.vibes.iam.dto.UpdateUserRequest;
import com.vibes.iam.dto.UserDto;
import com.vibes.iam.entity.Role;
import com.vibes.iam.entity.User;
import com.vibes.iam.exception.DuplicateResourceException;
import com.vibes.iam.exception.ResourceNotFoundException;
import com.vibes.iam.repository.RoleRepository;
import com.vibes.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("USER");
        testRole.setDescription("Regular User");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRoles(Set.of(testRole));

        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setPassword("Password@123");
        createUserRequest.setFirstName("New");
        createUserRequest.setLastName("User");
        createUserRequest.setRoles(Set.of("USER"));
    }

    @Test
    void getAllUsers_ShouldReturnPagedUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<UserDto> result = userService.getAllUsers(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByUsername_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void createUser_ShouldCreateUser_WhenValidRequest() {
        // Given
        when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createUserRequest.getPassword())).thenReturn("encodedPassword");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.createUser(createUserRequest, "admin");

        // Then
        assertNotNull(result);
        verify(userRepository).existsByUsername(createUserRequest.getUsername());
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(passwordEncoder).encode(createUserRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(auditService).logUserAction(eq(null), eq("admin"), eq("CREATE_USER"), eq("USER"), anyString(), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void createUser_ShouldThrowException_WhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, 
                () -> userService.createUser(createUserRequest, "admin"));
        verify(userRepository).existsByUsername(createUserRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        // Given
        when(userRepository.existsByUsername(createUserRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, 
                () -> userService.createUser(createUserRequest, "admin"));
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenValidRequest() {
        // Given
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        updateRequest.setRoles(Set.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername(updateRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.updateUser(1L, updateRequest, "admin");

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(auditService).logUserAction(eq(null), eq("admin"), eq("UPDATE_USER"), eq("USER"), anyString(), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L, "admin");

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
        verify(auditService).logUserAction(eq(null), eq("admin"), eq("DELETE_USER"), eq("USER"), anyString(), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void enableUser_ShouldEnableUser_WhenUserExists() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.enableUser(1L, "admin");

        // Then
        assertTrue(testUser.isEnabled());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(auditService).logUserAction(eq(null), eq("admin"), eq("ENABLE_USER"), eq("USER"), anyString(), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void disableUser_ShouldDisableUser_WhenUserExists() {
        // Given
        testUser.setEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.disableUser(1L, "admin");

        // Then
        assertFalse(testUser.isEnabled());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(auditService).logUserAction(eq(null), eq("admin"), eq("DISABLE_USER"), eq("USER"), anyString(), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void unlockUser_ShouldUnlockUser_WhenUserExists() {
        // Given
        testUser.setAccountNonLocked(false);
        testUser.setFailedLoginAttempts(5);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.unlockUser(1L, "admin");

        // Then
        assertTrue(testUser.isAccountNonLocked());
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLockedAt());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(auditService).logUserAction(eq(null), eq("admin"), eq("UNLOCK_USER"), eq("USER"), anyString(), eq(null), eq(null), eq(true), eq(null));
    }

    @Test
    void getUsersByRole_ShouldReturnUsers_WhenRoleExists() {
        // Given
        when(userRepository.findByRoleName("USER")).thenReturn(Arrays.asList(testUser));

        // When
        var result = userService.getUsersByRole("USER");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        verify(userRepository).findByRoleName("USER");
    }
}