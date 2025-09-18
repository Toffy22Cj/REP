package com.rep.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component // Asegúrate de que esta anotación está presente
public class SecurityDebugFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityDebugFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        logger.info("=== SECURITY DEBUG ===");
        logger.info("Endpoint: {} {}", request.getMethod(), request.getRequestURI());
        logger.info("Authenticated: {}", (auth != null && auth.isAuthenticated()));
        if (auth != null) {
            logger.info("Principal: {}", auth.getName());
            logger.info("Authorities: {}", auth.getAuthorities());
        }

        filterChain.doFilter(request, response);
    }
}