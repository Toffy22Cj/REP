package com.rep.security;

import com.rep.exception.ApiErrorWriter;
import com.rep.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitación de peticiones por IP (algoritmo token bucket, Bucket4j).
 *
 * <ul>
 *   <li>Login (API y panel web): 5 intentos cada 15 minutos.</li>
 *   <li>Resto de {@code /api/**}: 100 peticiones por minuto.</li>
 * </ul>
 *
 * <p>Las cabeceras {@code X-Forwarded-For}/{@code X-Real-IP} solo se tienen en cuenta si
 * {@code app.security.trust-proxy-headers=true} (despliegue detrás de un proxy inverso
 * de confianza). En caso contrario un atacante podría cambiar la cabecera en cada
 * petición y evadir el límite.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Evita crecimiento ilimitado de memoria si llegan peticiones desde muchas IPs. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final boolean trustProxyHeaders;
    private final long loginCapacity;
    private final long generalCapacity;

    public RateLimitFilter(
            @Value("${app.security.trust-proxy-headers:false}") boolean trustProxyHeaders,
            @Value("${app.security.rate-limit.login-per-15min:5}") long loginCapacity,
            @Value("${app.security.rate-limit.api-per-minute:100}") long generalCapacity) {
        this.trustProxyHeaders = trustProxyHeaders;
        this.loginCapacity = loginCapacity;
        this.generalCapacity = generalCapacity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();

        if (isLoginAttempt(request, path)) {
            if (!consume(loginBuckets, clientIp, loginBandwidth())) {
                logger.warn("Límite de intentos de login excedido desde IP {}", clientIp);
                ApiErrorWriter.write(request, response, ErrorCode.DEMASIADAS_SOLICITUDES, "Demasiados intentos de inicio de sesión. Espere 15 minutos");
                return;
            }
        } else if (path.startsWith("/api/") && !consume(generalBuckets, clientIp, generalBandwidth())) {
            logger.warn("Límite general de peticiones excedido desde IP {}", clientIp);
            ApiErrorWriter.write(request, response, ErrorCode.DEMASIADAS_SOLICITUDES);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isLoginAttempt(HttpServletRequest request, String path) {
        return "POST".equals(request.getMethod())
                && (path.equals("/api/auth/login") || path.equals("/admin/login"));
    }

    private Bandwidth loginBandwidth() {
        return Bandwidth.classic(loginCapacity, Refill.intervally(loginCapacity, Duration.ofMinutes(15)));
    }

    private Bandwidth generalBandwidth() {
        return Bandwidth.classic(generalCapacity, Refill.intervally(generalCapacity, Duration.ofMinutes(1)));
    }

    private static boolean consume(Map<String, Bucket> buckets, String key, Bandwidth limit) {
        if (buckets.size() >= MAX_TRACKED_CLIENTS && !buckets.containsKey(key)) {
            // Estrategia simple y segura: reiniciar el registro en lugar de crecer sin límite.
            buckets.clear();
        }
        return buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build()).tryConsume(1);
    }


    private String resolveClientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || path.equals("/error");
    }
}
