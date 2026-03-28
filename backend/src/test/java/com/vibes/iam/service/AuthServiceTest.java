package com.vibes.iam.service;

import com.vibes.iam.dto.AuthResponse;
import com.vibes.iam.dto.LoginRequest;
import com.vibes.iam.entity.User;
import com.vibes.iam.repository.UserRepository;
import com.vibes.iam.security.CustomUserDetails;
import com.vibes.iam.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setAccountNonExpired(true);
        testUser.setCredentialsNonExpired(true);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        userDetails = new CustomUserDetails(testUser);
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenValidCredentials() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(jwtUtil.generateToken(eq("testuser"), eq(1L), any()))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(eq("testuser"), eq(1L)))
                .thenReturn("refresh-token");

        // When
        AuthResponse result = authService.login(loginRequest, "127.0.0.1", "Mozilla/5.0");

        // Then
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(86400L, result.getExpiresIn());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(testUser);
        verify(jwtUtil).generateToken(eq("testuser"), eq(1L), any());
        verify(jwtUtil).generateRefreshToken(eq("testuser"), eq(1L));
        verify(auditService).logUserAction(eq(1L), eq("testuser"), eq("LOGIN"), eq(null), 
                anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"), eq(true), eq(null));
    }

    @Test
    void login_ShouldThrowException_AndLockAccount_WhenInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When & Then
        assertThrows(BadCredentialsException.class, 
                () -> authService.login(loginRequest, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(1, testUser.getFailedLoginAttempts());
        verify(userRepository).save(testUser);
        verify(auditService).logUserAction(eq(1L), eq("testuser"), eq("LOGIN_FAILED"), eq(null), 
                anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"), eq(false), eq("Invalid credentials"));
    }

    @Test
    void login_ShouldLockAccount_WhenMaxFailedAttemptsReached() {
        // Given
        testUser.setFailedLoginAttempts(4); // Already 4 failed attempts
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When & Then
        assertThrows(BadCredentialsException.class, 
                () -> authService.login(loginRequest, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(5, testUser.getFailedLoginAttempts());
        assertFalse(testUser.isAccountNonLocked());
        assertNotNull(testUser.getLockedAt());
        verify(userRepository).save(testUser);
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValidRefreshToken() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(refreshToken)).thenReturn(false);
        when(jwtUtil.getUsernameFromToken(refreshToken)).thenReturn("testuser");
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(eq("testuser"), eq(1L), any())).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(eq("testuser"), eq(1L))).thenReturn("new-refresh-token");

        // When
        AuthResponse result = authService.refreshToken(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        verify(jwtUtil).isTokenValid(refreshToken);
        verify(jwtUtil).isTokenExpired(refreshToken);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void refreshToken_ShouldThrowException_WhenInvalidRefreshToken() {
        // Given
        String invalidRefreshToken = "invalid-refresh-token";
        when(jwtUtil.isTokenValid(invalidRefreshToken)).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, 
                () -> authService.refreshToken(invalidRefreshToken));
        verify(jwtUtil).isTokenValid(invalidRefreshToken);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenExpiredRefreshToken() {
        // Given
        String expiredRefreshToken = "expired-refresh-token";
        when(jwtUtil.isTokenValid(expiredRefreshToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(expiredRefreshToken)).thenReturn(true);

        // When & Then
        assertThrows(BadCredentialsException.class, 
                () -> authService.refreshToken(expiredRefreshToken));
        verify(jwtUtil).isTokenValid(expiredRefreshToken);
        verify(jwtUtil).isTokenExpired(expiredRefreshToken);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenUserNotFound() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(refreshToken)).thenReturn(false);
        when(jwtUtil.getUsernameFromToken(refreshToken)).thenReturn("nonexistent");
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, 
                () -> authService.refreshToken(refreshToken));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void logout_ShouldLogAuditEvent_WhenValidUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        authService.logout("testuser", "127.0.0.1", "Mozilla/5.0");

        // Then
        verify(userRepository).findByUsername("testuser");
        verify(auditService).logUserAction(eq(1L), eq("testuser"), eq("LOGOUT"), eq(null), 
                eq("User logged out"), eq("127.0.0.1"), eq("Mozilla/5.0"), eq(true), eq(null));
    }

    @Test
    void logout_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, 
                () -> authService.logout("nonexistent", "127.0.0.1", "Mozilla/5.0"));
        verify(userRepository).findByUsername("nonexistent");
        verify(auditService, never()).logUserAction(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }
}