package com.campusos.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter for authentication endpoints, keyed by client IP.
 * Protects against brute-force login and token-refresh abuse.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/api/v1/auth/";
    private final int limitPerMinute;

    private final Cache<String, Window> windows = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Window>() {
                @Override
                public long expireAfterCreate(String key, Window w, long now) {
                    return Duration.ofMinutes(1).toNanos();
                }
                @Override
                public long expireAfterUpdate(String key, Window w, long now, long currentNanos) {
                    return currentNanos;
                }
                @Override
                public long expireAfterRead(String key, Window w, long now, long currentNanos) {
                    return currentNanos;
                }
            })
            .maximumSize(100_000)
            .build();

    public RateLimitFilter(@Value("${app.rate-limit.auth-requests-per-minute:20}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    private record Window(AtomicInteger count) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(request);
        Window window = windows.get(ip, k -> new Window(new AtomicInteger(0)));
        int count = window.count().incrementAndGet();
        if (count > limitPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Try again in a minute.\"},\"timestamp\":\""
                            + java.time.Instant.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        return fwd != null ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }
}
