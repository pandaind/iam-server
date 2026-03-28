package com.vibes.iam.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "testSecretKeyForJwtTokenGeneration");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 86400000L); // 24 hours
    }

    @Test
    void generateToken_ShouldGenerateValidToken() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS"),
                new SimpleGrantedAuthority("WRITE_USERS")
        );

        // When
        String token = jwtUtil.generateToken(username, userId, authorities);

        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtUtil.isTokenValid(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void generateRefreshToken_ShouldGenerateValidRefreshToken() {
        // Given
        String username = "testuser";
        Long userId = 1L;

        // When
        String refreshToken = jwtUtil.generateRefreshToken(username, userId);

        // Then
        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 0);
        assertTrue(jwtUtil.isTokenValid(refreshToken));
        assertFalse(jwtUtil.isTokenExpired(refreshToken));
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS")
        );
        String token = jwtUtil.generateToken(username, userId, authorities);

        // When
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void getUserIdFromToken_ShouldReturnCorrectUserId() {
        // Given
        String username = "testuser";
        Long userId = 123L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS")
        );
        String token = jwtUtil.generateToken(username, userId, authorities);

        // When
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void getAuthoritiesFromToken_ShouldReturnCorrectAuthorities() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS"),
                new SimpleGrantedAuthority("WRITE_USERS")
        );
        String token = jwtUtil.generateToken(username, userId, authorities);

        // When
        Collection<String> extractedAuthorities = jwtUtil.getAuthoritiesFromToken(token);

        // Then
        assertNotNull(extractedAuthorities);
        assertEquals(2, extractedAuthorities.size());
        assertTrue(extractedAuthorities.contains("READ_USERS"));
        assertTrue(extractedAuthorities.contains("WRITE_USERS"));
    }

    @Test
    void isTokenValid_ShouldReturnTrue_ForValidToken() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS")
        );
        String token = jwtUtil.generateToken(username, userId, authorities);

        // When
        boolean isValid = jwtUtil.isTokenValid(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtil.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForNullToken() {
        // When
        boolean isValid = jwtUtil.isTokenValid(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_ForFreshToken() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS")
        );
        String token = jwtUtil.generateToken(username, userId, authorities);

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void getUsernameFromToken_ShouldReturnNull_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        String username = jwtUtil.getUsernameFromToken(invalidToken);

        // Then
        assertNull(username);
    }

    @Test
    void getUserIdFromToken_ShouldReturnNull_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Long userId = jwtUtil.getUserIdFromToken(invalidToken);

        // Then
        assertNull(userId);
    }

    @Test
    void getAuthoritiesFromToken_ShouldReturnNull_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Collection<String> authorities = jwtUtil.getAuthoritiesFromToken(invalidToken);

        // Then
        assertNull(authorities);
    }

    @Test
    void generateToken_ShouldGenerateUniqueTokens() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("READ_USERS")
        );

        // When
        String token1 = jwtUtil.generateToken(username, userId, authorities);
        // Wait a moment to ensure different timestamps  
        try {
            Thread.sleep(1100); // Sleep longer than 1 second to ensure different iat timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String token2 = jwtUtil.generateToken(username, userId, authorities);

        // Then
        assertNotEquals(token1, token2);
        assertTrue(jwtUtil.isTokenValid(token1));
        assertTrue(jwtUtil.isTokenValid(token2));
    }

    @Test
    void generateToken_ShouldWorkWithEmptyAuthorities() {
        // Given
        String username = "testuser";
        Long userId = 1L;
        List<SimpleGrantedAuthority> authorities = Arrays.asList();

        // When
        String token = jwtUtil.generateToken(username, userId, authorities);

        // Then
        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        Collection<String> extractedAuthorities = jwtUtil.getAuthoritiesFromToken(token);
        assertNotNull(extractedAuthorities);
        assertTrue(extractedAuthorities.isEmpty());
    }

    @Test
    void refreshToken_ShouldNotContainAuthorities() {
        // Given
        String username = "testuser";
        Long userId = 1L;

        // When
        String refreshToken = jwtUtil.generateRefreshToken(username, userId);

        // Then
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.isTokenValid(refreshToken));
        assertEquals(username, jwtUtil.getUsernameFromToken(refreshToken));
        assertEquals(userId, jwtUtil.getUserIdFromToken(refreshToken));
        // Refresh token should not contain authorities
        Collection<String> authorities = jwtUtil.getAuthoritiesFromToken(refreshToken);
        assertTrue(authorities == null || authorities.isEmpty());
    }
}