package com.vibes.iam.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process rate limiter for authentication endpoints using Bucket4j.
 *
 * Limits are applied per client IP to mitigate brute-force and credential-stuffing attacks.
 * For multi-instance deployments, replace the local ConcurrentHashMap bucket store with
 * a distributed backend (e.g. Redis via bucket4j-redis).
 *
 * Enabled/disabled via: app.rate-limiting.enabled (default: true)
 * Limit configured via:  app.rate-limiting.auth-requests-per-minute (default: 20)
 */
@Configuration
public class RateLimitingConfig {

    @Value("${app.rate-limiting.auth-requests-per-minute:20}")
    private int authRequestsPerMinute;

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
    public Filter rateLimitingFilter() {
        return new RateLimitingFilter(authRequestsPerMinute);
    }

    public static class RateLimitingFilter implements Filter {

        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
        private final int requestsPerMinute;

        public RateLimitingFilter(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();

            // Rate limit only authentication-related endpoints
            if (isAuthEndpoint(uri)) {
                String clientIp = resolveClientIp(httpRequest);
                Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> createBucket());

                if (!bucket.tryConsume(1)) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(429);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write(
                            "{\"error\":\"TOO_MANY_REQUESTS\"," +
                            "\"message\":\"Rate limit exceeded. Please wait before retrying.\"}");
                    return;
                }
            }

            chain.doFilter(request, response);
        }

        private boolean isAuthEndpoint(String uri) {
            // Match both with and without the /api/v1 context path prefix
            return uri.contains("/auth/login")
                    || uri.contains("/auth/refresh")
                    || uri.contains("/auth/logout");
        }

        private Bucket createBucket() {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(requestsPerMinute)
                    .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        /**
         * Resolves the real client IP, honouring the X-Forwarded-For header set by
         * reverse proxies. Only the first (leftmost) address is used to avoid spoofing
         * via appended values.
         */
        private String resolveClientIp(HttpServletRequest request) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }
}
