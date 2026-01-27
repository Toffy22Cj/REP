# 🔒 Análisis de Seguridad - Sistema Educativo REP

## Resumen Ejecutivo

Se han identificado **13 vulnerabilidades críticas y mayores** que requieren atención inmediata, además de **8 áreas de mejora** para endurecimiento de seguridad.

---

## 🚨 VULNERABILIDADES CRÍTICAS

### 1. **Credenciales de Base de Datos Expuestas**

**Severidad:** 🔴 CRÍTICA  
**Ubicación:** `application.properties`

```properties
spring.datasource.username=admin
spring.datasource.password=admin
```

**Riesgo:** Las credenciales están en texto plano en el código fuente.

**Solución:**

```bash
# 1. Usar variables de entorno
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# 2. O usar Spring Cloud Config Server
# 3. O usar AWS Secrets Manager / Azure Key Vault
```

**Acción inmediata:**

```bash
# En production, establecer variables de entorno:
export DB_USERNAME="usuario_seguro"
export DB_PASSWORD="contraseña_fuerte_y_aleatoria"
```

---

### 2. **JWT Secret Débil y Expuesto**

**Severidad:** 🔴 CRÍTICA  
**Ubicación:** `application.properties`

```properties
jwt.secret=Cjppnaty22#UnaClaveMasLargaQueLlegueA32Chars
```

**Problemas:**

- La clave está en el código fuente
- Longitud insuficiente para HMAC-SHA256 (necesita >= 32 bytes/256 bits)
- Usa caracteres predecibles

**Solución:**

```properties
# application-prod.properties (NO commitar a Git)
jwt.secret=${JWT_SECRET}
jwt.expiration=3600000  # 1 hora en vez de 24 horas

# .gitignore
application-prod.properties
```

**Generar clave segura:**

```bash
# Linux/Mac
openssl rand -base64 64

# Resultado ejemplo:
# W7wKmZXkN9L2cQ5tP8vJ3kL4mN6qR9sT1uV2xW3yZ4aB5cD6eF7gH8iJ0kL1mN2oP3qR4sT5uV6wX7yZ8aB9cD0eF1gH2iJ3kL4mN5oP6qR7sT8uV9wX0yZ1aB2cD3eF4gH5iJ6kL7mN8oP9qR0
```

---

### 3. **SSL Deshabilitado en Conexión a BD**

**Severidad:** 🔴 CRÍTICA  
**Ubicación:** `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/colegio?useSSL=false
```

**Solución:**

```properties
# Production
spring.datasource.url=jdbc:mysql://localhost:3306/colegio?useSSL=true&serverSslMode=REQUIRED
spring.datasource.ssl-mode=REQUIRED
```

---

### 4. **CSRF Deshabilitado**

**Severidad:** 🔴 CRÍTICA  
**Ubicación:** `SecurityConfig.java`

```java
.csrf(AbstractHttpConfigurer::disable)  // ❌ Peligroso
```

**Solución:**

```java
// Para API REST con JWT, CSRF se puede desabilitar, pero mejor práctica:
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
// O mejor aún, para SPA/JS clients:
.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

---

### 5. **CORS Permisivo**

**Severidad:** 🟠 MAYOR  
**Ubicación:** `SecurityConfig.java`

```java
configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
configuration.setAllowedHeaders(Arrays.asList("*"));  // ❌ Demasiado permisivo
```

**Solución:**

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // En production
    String[] allowedOrigins = System.getenv("ALLOWED_ORIGINS")
        .split(",");  // ej: "https://app.example.com,https://admin.example.com"
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

### 6. **Logging Excesivamente Detallado**

**Severidad:** 🟠 MAYOR  
**Ubicación:** `application.properties`

```properties
logging.level.com.rep=TRACE
logging.level.org.springframework.security=DEBUG
spring.jpa.show-sql=true  # ❌ Expone queries SQL
```

**Problema:**

- `TRACE` expone detalles internos
- SQL visible puede revelar estructura de BD
- En debug se logean tokens y credenciales

**Solución:**

```properties
# Production
logging.level.root=WARN
logging.level.com.rep=INFO
logging.level.org.springframework.security=WARN
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Nunca loguear:
# - Tokens JWT
# - Contraseñas
# - Números de identificación
# - Datos sensibles de estudiantes
```

---

### 7. **DDL Automático en Production**

**Severidad:** 🟠 MAYOR  
**Ubicación:** `application.properties`

```properties
spring.jpa.hibernate.ddl-auto=update  # ❌ Peligroso en prod
```

**Solución:**

```properties
# application-prod.properties
spring.jpa.hibernate.ddl-auto=validate  # Solo validar

# Usar Liquibase o Flyway para migraciones controladas
```

**Implementar Flyway:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

---

### 8. **Sesiones HTTP Debug en Output**

**Severidad:** 🟠 MAYOR  
**Ubicación:** `SecurityDebugFilter.java`, `JwtAuthenticationFilter.java`

```java
System.out.println("DEBUG: Authorization Header: " + header);
System.out.println("DEBUG: Token authorities: " + authorities);
```

**Problema:** Imprime tokens y autoridades en consola.

**Solución:**

```java
// Solo en desarrollo
logger.debug("Token validation started for user: {}", username);  // Sin imprimir token

// Reemplazar todos System.out.println() con logger
```

---

### 9. **Validación de Token Incompleta**

**Severidad:** 🟠 MAYOR  
**Ubicación:** `JwtTokenHolder.java`

```java
public void setToken(String token) {
    try {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getBytes())  // ❌ Sin validar algoritmo
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
```

**Problemas:**

- No valida que la firma sea valida
- No verifica expiration si algo falla
- No usa SecretKey sino bytes

**Solución:**

```java
public void setToken(String token) {
    try {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("Token vacío");
        }

        String cleanToken = token.startsWith("Bearer ") ?
            token.substring(7) : token;

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)  // ✓ Usar SecretKey
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();

        // Validar expiration explícitamente
        if (claims.getExpiration().before(new Date())) {
            throw new ExpiredJwtException(null, claims, "Token expirado");
        }

        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            throw new InvalidTokenException("userId no presente en token");
        }

        this.userId = Long.parseLong(userIdObj.toString());

    } catch (JwtException e) {
        this.userId = null;
        this.token = null;
        throw new InvalidTokenException("Token inválido: " + e.getMessage(), e);
    }
}
```

---

### 10. **Tiempo de Expiración JWT Muy Largo**

**Severidad:** 🟡 MEDIA  
**Ubicación:** `application.properties`

```properties
jwt.expiration=86400000  # 24 horas ❌
```

**Solución:**

```properties
jwt.expiration=3600000  # 1 hora
jwt.refresh-token-expiration=604800000  # 7 días para refresh token

# Implementar refresh token mechanism
```

---

### 11. **Sin Rate Limiting**

**Severidad:** 🟡 MEDIA  
**Ubicación:** Configuración general

**Problema:** Vulnerable a ataques de fuerza bruta en login.

**Solución:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

```java
@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket loginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}

@Component
@Aspect
public class RateLimitAspect {
    @Around("@annotation(RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        String clientIp = getClientIp();
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Demasiados intentos. Intente más tarde.");
        }
        return joinPoint.proceed();
    }
}
```

---

### 12. **Sin Validación de Entrada**

**Severidad:** 🟡 MEDIA  
**Ubicación:** Controllers y DTOs

**Solución:**

```java
// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank(message = "Identificación requerida")
    @Size(min = 5, max = 20)
    private String identificacion;

    @NotBlank(message = "Contraseña requerida")
    @Size(min = 8, max = 100)
    private String password;
}

// Controller
@PostMapping("/auth/login")
public ResponseEntity<LoginResponse> login(
    @Valid @RequestBody LoginRequest request
) {
    // JPA ya valida automáticamente
}
```

---

### 13. **Exposición de Stack Traces**

**Severidad:** 🟡 MEDIA  
**Ubicación:** Manejo de excepciones

**Problema:** Los errores pueden revelar estructura de la aplicación.

**Solución:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        logger.error("Error interno", e);  // Log detallado solo en servidor

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "Error interno del servidor",  // Genérico para cliente
                "ID: " + UUID.randomUUID()     // Para tracking
            ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseError(DataIntegrityViolationException e) {
        logger.warn("Constraint violation", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Datos inválidos"));
    }
}
```

---

## 📋 MEJORAS RECOMENDADAS (Sin vulnerabilidades inmediatas)

### 1. **Implementar HTTPS/TLS**

```properties
server.ssl.key-store=${SSL_KEYSTORE_PATH}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.enabled=true
```

### 2. **Headers de Seguridad HTTP**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
        .xssProtection()
        .and()
        .contentSecurityPolicy("default-src 'self'")
        .and()
        .cacheControl()
        .and()
        .frameOptions().deny()
        .and()
        .hsts()
            .maxAgeInSeconds(31536000)
            .includeSubDomains(true)
    );

    return http.build();
}
```

### 3. **Auditoría y Logging**

```java
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    private Long id;
    private String usuario;
    private String accion;
    private String recurso;
    private String resultado;  // SUCCESS / FAILURE
    private LocalDateTime timestamp;
    private String ipAddress;
}

// AOP para loguear cambios
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        // Log acción antes y después
    }
}
```

### 4. **Encriptación de Datos Sensibles**

```java
@Entity
public class Usuario {
    @Encrypted  // Usando Jasypt
    private String numeroIdentificacion;

    @Encrypted
    private String email;
}

<!-- pom.xml -->
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

### 5. **Password Policy Fuerte**

```java
@Component
public class PasswordValidator {
    public void validate(String password) {
        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{12,}$")) {
            throw new PasswordPolicyException(
                "La contraseña debe tener: " +
                "- Mínimo 12 caracteres\n" +
                "- 1 mayúscula\n" +
                "- 1 minúscula\n" +
                "- 1 número\n" +
                "- 1 carácter especial (@#$%^&+=)"
            );
        }
    }
}
```

### 6. **Monitoreo y Alertas**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```

```properties
management.endpoints.web.exposure.include=metrics,health,prometheus
management.endpoint.health.show-details=when-authorized
```

### 7. **Validación de Archivo en Uploads**

```java
// AsistenciaController, ProfesorController, etc.
private void validarArchivo(MultipartFile file) {
    if (file.getSize() > 10_000_000) {  // 10MB
        throw new FileSizeExceededException("Archivo muy grande");
    }

    String contentType = file.getContentType();
    List<String> permitidos = Arrays.asList("application/pdf", "image/jpeg", "image/png");
    if (!permitidos.contains(contentType)) {
        throw new InvalidFileTypeException("Tipo de archivo no permitido");
    }

    // Validar por magia bytes, no por extensión
    byte[] header = new byte[4];
    file.getInputStream().read(header);
    if (!isValidPdf(header) && !isValidImage(header)) {
        throw new InvalidFileContentException("Archivo inválido");
    }
}
```

### 8. **Control de Acceso por Recurso**

```java
@Service
public class AccessControlService {

    public void validarAcceso(Usuario usuario, Long estudianteId) {
        if (usuario.getRol() == Rol.ESTUDIANTE &&
            !usuario.getId().equals(estudianteId)) {
            throw new AccessDeniedException(
                "No tienes permiso para acceder a este recurso"
            );
        }
    }

    public void validarAccesoProfesor(Usuario profesor, Long cursoId) {
        boolean esProfesorDelCurso = profesorRepository
            .existsByIdAndCursos_Id(profesor.getId(), cursoId);
        if (!esProfesorDelCurso) {
            throw new AccessDeniedException(
                "No eres profesor de este curso"
            );
        }
    }
}
```

---

## 🔧 Plan de Implementación

### Fase 1 - INMEDIATA (Semana 1)

1. ✅ Mover credenciales a variables de entorno
2. ✅ Generar JWT secret fuerte
3. ✅ Habilitar SSL en BD
4. ✅ Remover System.out.println() de seguridad

### Fase 2 - URGENTE (Semana 2)

5. ✅ Implementar rate limiting
6. ✅ Validación de entrada en todos los DTOs
7. ✅ Global exception handler
8. ✅ Reducir logging en production

### Fase 3 - IMPORTANTE (Semana 3-4)

9. ✅ HTTPS/TLS
10. ✅ Security headers
11. ✅ Auditoría completa
12. ✅ Encriptación de datos sensibles

### Fase 4 - MEJORA CONTINUA

13. ✅ Monitoreo y alertas
14. ✅ Penetration testing
15. ✅ Dependency scanning (OWASP Dependency-Check)

---

## 📊 Checklist de Seguridad Pre-Producción

- [ ] Todas las credenciales en variables de entorno
- [ ] SSL/TLS habilitado (certificado valido)
- [ ] Rate limiting en endpoints de login
- [ ] Validación de entrada completa
- [ ] Logging sin datos sensibles
- [ ] CORS restringido a dominios conocidos
- [ ] Headers de seguridad configurados
- [ ] Timeout de sesión implementado
- [ ] Auditoría de acciones sensibles
- [ ] Backup de BD automático
- [ ] Monitoreo de seguridad activo
- [ ] Plan de incidentes documentado
- [ ] Dependencias sin CVEs (verificar con OWASP)
- [ ] PENETRATION TESTING completado

---

## 🛠 Herramientas Recomendadas

```bash
# Verificar dependencias por CVEs
mvn dependency-check:check

# OWASP ZAP para testing
docker run -t owasp/zap2docker-stable zap-baseline.py -t http://localhost:8080

# SonarQube para análisis estático
docker run -d --name sonarqube -p 9000:9000 sonarqube:latest

# Verificar headers HTTP
curl -I https://your-domain.com
```

---

## 📚 Referencias

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

---

**Última revisión:** 25 de enero de 2026  
**Próxima revisión:** Después de implementar Fase 1
