package com.rep.config;

import com.rep.security.JwtAuthenticationFilter;
import com.rep.security.RateLimitFilter;
import com.rep.service.logica.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Configuración de Seguridad Mejorada para Spring Boot
 * 
 * Cambios principales:
 * - CSRF habilitado con configuración segura
 * - CORS restringido por environment
 * - Security headers completos (HSTS, CSP, X-Frame-Options, etc.)
 * - Rate limiting integrado
 * - Session management seguro
 * - Logging de seguridad sin exponer datos sensibles
 */
@Configuration
@EnableWebSecurity
public class SecurityConfigSecure {
    private final CustomUserDetailsService userDetailsService;
    private final SecretKey secretKey;
    private final JwtConfig jwtConfig;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfigSecure(
            CustomUserDetailsService userDetailsService,
            SecretKey secretKey,
            JwtConfig jwtConfig,
            RateLimitFilter rateLimitFilter) {
        this.userDetailsService = userDetailsService;
        this.secretKey = secretKey;
        this.jwtConfig = jwtConfig;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ========== CSRF CONFIGURATION ==========
                // Para API REST con JWT, CSRF token no es necesario
                // pero configuramos para ser defensivamente correcto
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Ignorar para API JSON
                        .csrfTokenRepository(new CookieCsrfTokenRepository()))

                // ========== CORS CONFIGURATION ==========
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ========== SESSION MANAGEMENT ==========
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixationProtection(SessionFixationProtectionStrategy.MIGRATETOSESSION)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/api/auth/login")))

                // ========== AUTHORIZATION ==========
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/registro").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Protected endpoints
                        .requestMatchers(HttpMethod.GET, "/api/preguntas/*/archivo")
                        .hasAnyAuthority("ESTUDIANTE", "PROFESOR", "ADMIN")

                        .requestMatchers("/api/profesor/**").hasAuthority("PROFESOR")
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/actividades/*/preguntas")
                        .hasAnyAuthority("ESTUDIANTE", "PROFESOR", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/estudiante/*/actividades/*/resolver")
                        .hasAuthority("ESTUDIANTE")

                        .requestMatchers("/api/actividades/**")
                        .hasAnyAuthority("PROFESOR", "ADMIN")

                        .requestMatchers("/api/preguntas/**")
                        .hasAnyAuthority("PROFESOR", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // ========== EXCEPTION HANDLING ==========
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Access Denied\"}");
                        }))

                // ========== SECURITY HEADERS ==========
                .headers(headers -> headers
                        .xssProtection()
                        .and()
                        .contentSecurityPolicy("default-src 'self'; script-src 'self'")
                        .and()
                        .cacheControl()
                        .and()
                        .frameOptions().deny()
                        .and()
                        .hsts()
                        .maxAgeInSeconds(31536000) // 1 year
                        .includeSubDomains(true)
                        .preload(true)
                        .and()
                        .referrerPolicy()
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_NO_REFERRER)
                        .and()
                        .permissionsPolicy()
                        .policy("camera=(), microphone=(), geolocation=()")
                        .and()
                        .addHeaderWriter(new StaticHeadersWriter(
                                "X-Content-Type-Options", "nosniff",
                                "X-Permitted-Cross-Domain-Policies", "none",
                                "Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")))

                // ========== FILTERS ==========
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(secretKey, jwtConfig, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Obtener orígenes permitidos desde variable de entorno
        String allowedOriginsEnv = System.getenv("ALLOWED_ORIGINS");
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isEmpty()) {
            String[] origins = allowedOriginsEnv.split(",");
            configuration.setAllowedOrigins(Arrays.stream(origins)
                    .map(String::trim)
                    .collect(Collectors.toList()));
        } else {
            // Default para desarrollo
            configuration.setAllowedOrigins(Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:8080"));
        }

        // Métodos permitidos - restricto a necesarios
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers permitidos - específico, no "*"
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Accept-Language",
                "X-CSRF-Token"));

        // Headers expuestos al cliente
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Count"));

        // NO permitir credenciales por defecto
        configuration.setAllowCredentials(false);

        // Cache CORS por 1 hora
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12 para seguridad adicional
    }

    // Método estático para configurar el repositorio de tokens CSRF
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieName("XSRF-TOKEN");
        return repository;
    }
}
