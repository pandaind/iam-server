package com.vibes.iam.service;

import com.vibes.iam.dto.UserSessionDto;
import com.vibes.iam.entity.User;
import com.vibes.iam.entity.UserSession;
import com.vibes.iam.exception.ResourceNotFoundException;
import com.vibes.iam.repository.UserRepository;
import com.vibes.iam.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SessionService {

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Value("${app.security.session.max-sessions}")
    private int maxSessions;

    @Value("${app.security.session.timeout}")
    private int sessionTimeout;

    public String createSession(String username, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<UserSession> activeSessions = sessionRepository.findActiveSessionsByUserId(user.getId(), LocalDateTime.now());
        
        if (activeSessions.size() >= maxSessions) {
            UserSession oldestSession = activeSessions.stream()
                    .min((s1, s2) -> s1.getLastAccessedAt().compareTo(s2.getLastAccessedAt()))
                    .orElse(null);
            
            if (oldestSession != null) {
                invalidateSession(oldestSession.getSessionId());
            }
        }

        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(sessionTimeout);
        
        UserSession session = new UserSession(sessionId, user, ipAddress, userAgent, expiresAt);
        sessionRepository.save(session);

        auditService.logUserAction(user.getId(), username, "SESSION_CREATED", "SESSION", 
                "Session created", ipAddress, userAgent, true, null);

        return sessionId;
    }

    public void updateSessionActivity(String sessionId) {
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        if (session != null && session.isActive() && !session.isExpired()) {
            session.setLastAccessedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTimeout));
            sessionRepository.save(session);
        }
    }

    public void invalidateSession(String sessionId) {
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        if (session != null) {
            session.setActive(false);
            sessionRepository.save(session);

            auditService.logUserAction(session.getUser().getId(), session.getUser().getUsername(), 
                    "SESSION_INVALIDATED", "SESSION", "Session invalidated", null, null, true, null);
        }
    }

    public void invalidateAllUserSessions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<UserSession> activeSessions = sessionRepository.findByUserIdAndActiveTrue(user.getId());
        for (UserSession session : activeSessions) {
            session.setActive(false);
        }
        sessionRepository.saveAll(activeSessions);

        auditService.logUserAction(user.getId(), username, "ALL_SESSIONS_INVALIDATED", "SESSION", 
                "All sessions invalidated", null, null, true, null);
    }

    public boolean isSessionValid(String sessionId) {
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        return session != null && session.isActive() && !session.isExpired();
    }

    public List<UserSessionDto> getUserActiveSessions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return sessionRepository.findActiveSessionsByUserId(user.getId(), LocalDateTime.now())
                .stream()
                .map(UserSessionDto::new)
                .collect(Collectors.toList());
    }

    public void invalidateUserSession(String username, String sessionId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Session does not belong to user");
        }

        session.setActive(false);
        sessionRepository.save(session);

        auditService.logUserAction(user.getId(), username, "SESSION_INVALIDATED_BY_USER", "SESSION", 
                "Session invalidated by user", null, null, true, null);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSessions() {
        List<UserSession> expiredSessions = sessionRepository.findExpiredSessions(LocalDateTime.now());
        
        for (UserSession session : expiredSessions) {
            session.setActive(false);
        }
        
        sessionRepository.saveAll(expiredSessions);
        
        if (!expiredSessions.isEmpty()) {
            auditService.logUserAction(null, "SYSTEM", "EXPIRED_SESSIONS_CLEANUP", "SESSION", 
                    "Cleaned up " + expiredSessions.size() + " expired sessions", null, null, true, null);
        }
    }
}