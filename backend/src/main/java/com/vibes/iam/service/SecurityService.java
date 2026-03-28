package com.vibes.iam.service;

import com.vibes.iam.entity.User;
import com.vibes.iam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class SecurityService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;

    public boolean isAccountLocked(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        if (!user.isAccountNonLocked()) {
            if (user.getLockedAt() != null && 
                user.getLockedAt().plusMinutes(LOCKOUT_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                return true;
            } else {
                unlockAccount(username);
                return false;
            }
        }

        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt != null && attempt.getAttempts() >= MAX_LOGIN_ATTEMPTS) {
            if (user.getFailedLoginAttempts() == 0) {
                // DB was manually reset (admin unlock) — synchronize in-memory state
                loginAttempts.remove(username);
                return false;
            }
            if (attempt.getLastAttempt().plusMinutes(LOCKOUT_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                lockAccount(username);
                return true;
            } else {
                loginAttempts.remove(username);
            }
        }

        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(String username, String ipAddress) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(username, k -> new LoginAttempt());
        attempt.incrementAttempts();
        attempt.setLastAttempt(LocalDateTime.now());
        attempt.setIpAddress(ipAddress);

        if (attempt.getAttempts() >= MAX_LOGIN_ATTEMPTS) {
            lockAccount(username);
            auditService.logUserAction(null, username, "ACCOUNT_LOCKED", "SECURITY", 
                    "Account locked due to failed login attempts from " + ipAddress, ipAddress, null, true, null);
        }

        auditService.logUserAction(null, username, "FAILED_LOGIN_ATTEMPT", "SECURITY", 
                "Failed login attempt " + attempt.getAttempts() + " from " + ipAddress, ipAddress, null, false, null);
    }

    public void recordSuccessfulLogin(String username) {
        loginAttempts.remove(username);
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setFailedLoginAttempts(0);
            if (!user.isAccountNonLocked()) {
                user.setAccountNonLocked(true);
                user.setLockedAt(null);
            }
            userRepository.save(user);
        }
    }

    private void lockAccount(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setAccountNonLocked(false);
            user.setLockedAt(LocalDateTime.now());
            user.setFailedLoginAttempts(loginAttempts.get(username).getAttempts());
            userRepository.save(user);
        }
    }

    private void unlockAccount(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setAccountNonLocked(true);
            user.setLockedAt(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            loginAttempts.remove(username);
            
            auditService.logUserAction(user.getId(), username, "ACCOUNT_UNLOCKED", "SECURITY", 
                    "Account automatically unlocked after lockout period", null, null, true, null);
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            auditService.logUserAction(user.getId(), username, "PASSWORD_CHANGE_FAILED", "SECURITY", 
                    "Failed password change - incorrect old password", null, null, false, "Incorrect old password");
            return false;
        }

        PasswordPolicyService.PasswordValidationResult validation = passwordPolicyService.validatePassword(newPassword);
        if (!validation.isValid()) {
            auditService.logUserAction(user.getId(), username, "PASSWORD_CHANGE_FAILED", "SECURITY", 
                    "Failed password change - policy violation", null, null, false, String.join(", ", validation.getErrors()));
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        auditService.logUserAction(user.getId(), username, "PASSWORD_CHANGED", "SECURITY", 
                "Password successfully changed", null, null, true, null);

        return true;
    }

    public void forcePasswordReset(String username, String adminUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setCredentialsNonExpired(false);
        userRepository.save(user);

        auditService.logUserAction(user.getId(), adminUsername, "FORCE_PASSWORD_RESET", "SECURITY", 
                "Forced password reset for user: " + username, null, null, true, null);
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupOldLoginAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);
        loginAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff));
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkPasswordExpiration() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<User> usersWithExpiredPasswords = userRepository.findUsersWithExpiredPasswords(threeMonthsAgo);
        
        for (User user : usersWithExpiredPasswords) {
            user.setCredentialsNonExpired(false);
            userRepository.save(user);
            
            auditService.logUserAction(user.getId(), "SYSTEM", "PASSWORD_EXPIRED", "SECURITY", 
                    "Password expired for user: " + user.getUsername(), null, null, true, null);
        }
        
        if (!usersWithExpiredPasswords.isEmpty()) {
            auditService.logUserAction(null, "SYSTEM", "PASSWORD_EXPIRATION_CHECK", "SECURITY", 
                    "Expired passwords for " + usersWithExpiredPasswords.size() + " users", null, null, true, null);
        }
    }

    private static class LoginAttempt {
        private int attempts = 0;
        private LocalDateTime lastAttempt;
        private String ipAddress;

        public void incrementAttempts() {
            this.attempts++;
        }

        public int getAttempts() {
            return attempts;
        }

        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }

        public void setLastAttempt(LocalDateTime lastAttempt) {
            this.lastAttempt = lastAttempt;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }
}