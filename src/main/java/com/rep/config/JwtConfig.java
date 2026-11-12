package com.rep.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Configuración de JWT. El secreto se lee de {@code jwt.secret} (variable de entorno
 * {@code JWT_SECRET}) y nunca debe versionarse.
 */
@Configuration
public class JwtConfig {

    /** HS256 requiere al menos 256 bits de clave. */
    static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Bean
    public SecretKey secretKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret no está configurado. Defina la variable de entorno JWT_SECRET "
                            + "(p. ej. generada con: openssl rand -base64 64).");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " bytes. Defina JWT_SECRET con un valor más largo.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public Long getExpiration() {
        return expiration;
    }
}
