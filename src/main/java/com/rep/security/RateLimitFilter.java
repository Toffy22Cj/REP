package com.rep.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter para prevenir ataques de fuerza bruta
 * 
 * Implementa token bucket algorithm (Bucket4j)
 * Limita intentos por IP address
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    // 5 intentos por 15 minutos para login
    private static final Bandwidth LOGIN_LIMIT = 
        Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
    
    // 100 requests por minuto para otros endpoints
    private static final Bandwidth GENERAL_LIMIT = 
        Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIP = getClientIP(request);
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Rate limit específico para login
            if (requestPath.contains("/api/auth/login") && "POST".equals(method)) {
                if (!checkLoginLimit(clientIP)) {
                    logger.warn("Rate limit exceeded for login attempt from IP: {}", clientIP);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Demasiados intentos. Intente más tarde.\"}");
                    return;
                }
            } 
            // Rate limit general para otros endpoints
            else if (requestPath.startsWith("/api/")) {
                if (!checkGeneralLimit(clientIP)) {
                    logger.warn("General rate limit exceeded from IP: {}", clientIP);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Rate limit exceeded.\"}");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error in rate limit filter: ", e);
            filterChain.doFilter(request, response);
        }
    }

    private boolean checkLoginLimit(String clientIP) {
        return loginBuckets
            .computeIfAbsent(clientIP, k -> Bucket4j.builder()
                .addLimit(LOGIN_LIMIT)
                .build())
            .tryConsume(1);
    }

    private boolean checkGeneralLimit(String clientIP) {
        return generalBuckets
            .computeIfAbsent(clientIP, k -> Bucket4j.builder()
                .addLimit(GENERAL_LIMIT)
                .build())
            .tryConsume(1);
    }

    private String getClientIP(HttpServletRequest request) {
        // Obtener IP real cuando está detrás de proxy
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Tomar el primer IP si hay múltiples
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // No aplicar rate limit a healthchecks
        String path = request.getRequestURI();
        return path.contains("/actuator/health") || path.contains("/error");
    }
}
