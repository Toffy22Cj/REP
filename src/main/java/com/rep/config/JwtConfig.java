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
            throw new IllegalStateException("""

                    ================================================================
                     Falta JWT_SECRET: la aplicación no puede arrancar sin él.

                     Solución rápida (desde la raíz del proyecto):
                       cp .env.example .env
                       echo "JWT_SECRET=$(openssl rand -base64 48)" >> .env
                       mvn spring-boot:run

                     O como variable de entorno:
                       export JWT_SECRET="$(openssl rand -base64 48)"
                    ================================================================
                    """);
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " caracteres (tiene " + bytes.length
                            + "). Genere uno con: openssl rand -base64 48");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public Long getExpiration() {
        return expiration;
    }
}
