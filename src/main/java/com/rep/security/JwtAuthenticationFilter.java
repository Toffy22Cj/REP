package com.rep.security;

import com.rep.config.JwtConfig;
import com.rep.service.logica.CustomUserDetailsService;
import io.jsonwebtoken.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SecretKey secretKey;
    private final JwtConfig jwtConfig;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(SecretKey secretKey,
            JwtConfig jwtConfig,
            CustomUserDetailsService userDetailsService) {
        this.secretKey = secretKey;
        this.jwtConfig = jwtConfig;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        System.out.println("DEBUG: Incoming request to " + request.getRequestURI());
        System.out.println("DEBUG: Authorization Header: "
                + (header != null ? header.substring(0, Math.min(header.length(), 15)) + "..." : "NULL"));

        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("DEBUG: No Bearer token found, skipping filter");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            System.out.println("DEBUG: Validating token...");
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();

            // 1. Extraer autoridades del token
            List<SimpleGrantedAuthority> authorities = ((List<?>) claims.get("authorities"))
                    .stream()
                    .map(authority -> new SimpleGrantedAuthority((String) authority))
                    .collect(Collectors.toList());

            System.out.println("DEBUG: Token authorities: " + authorities);

            // 2. Cargar UserDetails desde la BD para asegurar la consistencia
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            System.out.println("DEBUG: DB user authorities: " + userDetails.getAuthorities());

            // 3. Crear autenticación con el UserDetails y las autoridades del token
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, // Usar userDetails en lugar del username
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("DEBUG: User authenticated successfully: " + username);

        } catch (Exception e) {
            System.out.println("DEBUG: Authentication failed: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}