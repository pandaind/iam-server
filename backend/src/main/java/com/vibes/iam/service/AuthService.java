package com.vibes.iam.service;

import com.vibes.iam.dto.AuthResponse;
import com.vibes.iam.dto.LoginRequest;
import com.vibes.iam.dto.UserDto;
import com.vibes.iam.entity.User;
import com.vibes.iam.repository.UserRepository;
import com.vibes.iam.security.CustomUserDetails;
import com.vibes.iam.security.JwtUtil;
import com.vibes.iam.security.TokenBlacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    public AuthResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), 
                    loginRequest.getPassword()
                )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            securityService.recordSuccessfulLogin(user.getUsername());
            sessionService.createSession(user.getUsername(), ipAddress, userAgent);

            String accessToken = jwtUtil.generateToken(
                user.getUsername(), 
                user.getId(), 
                userDetails.getAuthorities()
            );
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());

            auditService.logUserAction(user.getId(), user.getUsername(), "LOGIN", null, 
                "User logged in successfully", ipAddress, userAgent, true, null);

            return new AuthResponse(accessToken, refreshToken, jwtExpiration / 1000, new UserDto(user));

        } catch (DisabledException e) {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                auditService.logUserAction(user.getId(), user.getUsername(), "LOGIN_FAILED", null, 
                    "Login failed - account disabled", ipAddress, userAgent, false, "Account disabled");
            }
            throw new BadCredentialsException("Invalid credentials");
        } catch (LockedException e) {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                auditService.logUserAction(user.getId(), user.getUsername(), "LOGIN_FAILED", null, 
                    "Login failed - account locked", ipAddress, userAgent, false, "Account locked");
            }
            throw new BadCredentialsException("Invalid credentials");
        } catch (AccountExpiredException e) {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                auditService.logUserAction(user.getId(), user.getUsername(), "LOGIN_FAILED", null, 
                    "Login failed - account expired", ipAddress, userAgent, false, "Account expired");
            }
            throw new BadCredentialsException("Invalid credentials");
        } catch (CredentialsExpiredException e) {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                auditService.logUserAction(user.getId(), user.getUsername(), "LOGIN_FAILED", null, 
                    "Login failed - credentials expired", ipAddress, userAgent, false, "Credentials expired");
            }
            throw new BadCredentialsException("Invalid credentials");
        } catch (BadCredentialsException e) {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                securityService.recordFailedLogin(user.getUsername(), ipAddress);
                auditService.logUserAction(user.getId(), user.getUsername(), "LOGIN_FAILED", null,
                    "Failed login attempt", ipAddress, userAgent, false, "Invalid credentials");
            }
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        if (tokenBlacklist.isBlacklisted(refreshToken)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtUtil.generateToken(username, userId, userDetails.getAuthorities());
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

        return new AuthResponse(newAccessToken, newRefreshToken, jwtExpiration / 1000, new UserDto(user));
    }

    public void logout(String username, String token, String refreshToken, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (token != null) {
            tokenBlacklist.blacklist(token, jwtUtil.getExpirationFromToken(token));
        }
        if (refreshToken != null) {
            tokenBlacklist.blacklist(refreshToken, jwtUtil.getExpirationFromToken(refreshToken));
        }
        sessionService.invalidateAllUserSessions(username);

        auditService.logUserAction(user.getId(), username, "LOGOUT", null,
            "User logged out", ipAddress, userAgent, true, null);
    }
}