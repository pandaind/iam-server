package com.vibes.iam.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "app.security.headers.enabled", havingValue = "true", matchIfMissing = true)
    public Filter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    public static class SecurityHeadersFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String requestURI = httpRequest.getRequestURI();

            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
            
            if (httpRequest.isSecure()) {
                httpResponse.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload");
            }

            // Allow frames for H2 console in test environment
            if (requestURI.startsWith("/h2-console")) {
                httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
                String contentSecurityPolicy = "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'self'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'";
                httpResponse.setHeader("Content-Security-Policy", contentSecurityPolicy);
            } else {
                httpResponse.setHeader("X-Frame-Options", "DENY");
                String contentSecurityPolicy = "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'";
                httpResponse.setHeader("Content-Security-Policy", contentSecurityPolicy);
            }

            chain.doFilter(request, response);
        }
    }
}