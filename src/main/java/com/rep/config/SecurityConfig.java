package com.rep.config;

import com.rep.security.JwtAuthenticationFilter;
import com.rep.security.RateLimitFilter;
import com.rep.service.logica.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad.
 *
 * <ol>
 *   <li><b>API REST</b> (cliente de escritorio): JWT sin estado.</li>
 *   <li><b>Administración</b> ({@code /admin/**} y {@code /api/admin/**}): sesión con
 *       formulario para el panel web <i>y</i> JWT para el cliente de escritorio.</li>
 *   <li><b>Resto</b>: recursos estáticos públicos; todo lo demás autenticado.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String ADMIN = "ADMIN";
    private static final String PROFESOR = "PROFESOR";
    private static final String ESTUDIANTE = "ESTUDIANTE";

    private final CustomUserDetailsService userDetailsService;
    private final SecretKey secretKey;
    private final RateLimitFilter rateLimitFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            SecretKey secretKey,
            RateLimitFilter rateLimitFilter,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}") String allowedOrigins) {
        this.userDetailsService = userDetailsService;
        this.secretKey = secretKey;
        this.rateLimitFilter = rateLimitFilter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/**", "/api/public/**", "/api/estudiante/**", "/api/profesor/**",
                        "/api/actividades/**", "/api/preguntas/**", "/api/registro")
                .csrf(AbstractHttpConfigurer::disable) // API sin estado con Bearer token: CSRF no aplica
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.deny()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/api/auth/**", "/api/registro").permitAll()
                        // Descarga de adjuntos: el controlador valida la pertenencia al curso/actividad
                        .requestMatchers(HttpMethod.GET, "/api/preguntas/*/archivo", "/api/preguntas/opciones/*/archivo")
                        .hasAnyAuthority(ESTUDIANTE, PROFESOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/actividades/*/preguntas")
                        .hasAnyAuthority(ESTUDIANTE, PROFESOR, ADMIN)
                        .requestMatchers("/api/estudiante/**").hasAnyAuthority(ESTUDIANTE, ADMIN)
                        .requestMatchers("/api/profesor/**").hasAuthority(PROFESOR)
                        .requestMatchers("/api/actividades/**", "/api/preguntas/**").hasAnyAuthority(PROFESOR, ADMIN)
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**", "/api/admin/**")
                // La cookie de sesión se emite con SameSite=Strict (ver application.properties), lo que
                // impide que otro sitio dispare peticiones autenticadas (CSRF). Las peticiones JWT no
                // usan cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.deny()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login.html", "/admin/css/**", "/admin/js/**").permitAll()
                        .anyRequest().hasAuthority(ADMIN))
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/admin/**")))
                .formLogin(form -> form
                        .loginPage("/admin/login.html")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/index.html", true)
                        .failureUrl("/admin/login.html?error")
                        .usernameParameter("identificacion")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login.html")
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                // Permite que el cliente de escritorio use /api/admin/** con su token JWT
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/assets/**", "/error")
                        .permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(secretKey, userDetailsService);
    }

    /*
     * Los filtros declarados como beans se registrarían además como filtros globales del
     * contenedor de servlets (fuera de Spring Security). Solo deben ejecutarse dentro de
     * las cadenas de seguridad configuradas arriba.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
