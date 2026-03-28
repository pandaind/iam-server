package com.vibes.iam.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT token blacklist for immediate revocation on logout.
 * Entries are evicted automatically once the token's natural expiry passes.
 */
@Component
public class TokenBlacklist {

    // token → expiry time in millis
    private final Map<String, Long> blacklisted = new ConcurrentHashMap<>();

    public void blacklist(String token, Date expiresAt) {
        if (token != null && expiresAt != null) {
            blacklisted.put(token, expiresAt.getTime());
        }
    }

    public boolean isBlacklisted(String token) {
        return blacklisted.containsKey(token);
    }

    /** Remove entries whose tokens have already expired naturally (no longer needed). */
    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void evictExpired() {
        long now = System.currentTimeMillis();
        blacklisted.entrySet().removeIf(e -> e.getValue() < now);
    }
}
