# 📋 PLAN DE IMPLEMENTACIÓN DE MEJORAS DE SEGURIDAD

## Fase 1: CRÍTICA (Implementar Inmediatamente - Semana 1)

### 1.1 Externalizar Credenciales ✅

**Tiempo estimado:** 2 horas

```bash
# 1. Crear archivo .env.local (NO SUBIR A GIT)
touch .env.local
chmod 600 .env.local

# 2. Agregar a .gitignore
echo ".env.local" >> .gitignore
echo "application-prod.properties" >> .gitignore

# 3. Editar .env.local con valores reales
cat > .env.local << 'EOF'
export DB_USERNAME="usuario_bd_seguro"
export DB_PASSWORD="contraseña_super_fuerte_16_caracteres_minimo"
export JWT_SECRET="generar_con_openssl_rand_base64_64"
EOF

# 4. Cargar variables antes de ejecutar
source .env.local
```

**Archivos a modificar:**

- `application-prod.properties` ← Ya creado

### 1.2 Generar JWT Secret Fuerte ✅

**Tiempo estimado:** 15 minutos

```bash
# Generar clave de 256 bits (32 bytes)
openssl rand -base64 64

# Usar en .env.local
# JWT_SECRET="<resultado_del_comando_anterior>"

# Cambiar en application-prod.properties
# jwt.secret=${JWT_SECRET}
```

### 1.3 Habilitar SSL en Base de Datos ✅

**Tiempo estimado:** 30 minutos

```properties
# application-prod.properties
spring.datasource.url=jdbc:mysql://localhost:3306/colegio?useSSL=true&serverSslMode=REQUIRED&allowPublicKeyRetrieval=false
```

### 1.4 Remover System.out.println() de Debug ✅

**Tiempo estimado:** 1 hora

```bash
# Buscar todos los System.out.println relacionados a seguridad
grep -r "System.out.println" src/main/java --include="*.java" | grep -i "token\|password\|debug\|auth\|secret"

# Reemplazar con logger.debug()
# Archivos a modificar:
# - JwtAuthenticationFilter.java
# - SecurityDebugFilter.java (considerar remover completamente)
# - AsistenciaController.java
# - ProfesorController.java
```

### 1.5 Configurar Logging de Producción ✅

**Tiempo estimado:** 30 minutos

```properties
# application-prod.properties (Ya está creado)
logging.level.root=WARN
logging.level.com.rep=INFO
logging.level.org.springframework.security=WARN
spring.jpa.show-sql=false
```

---

## Fase 2: URGENTE (Implementar en Semana 2)

### 2.1 Implementar Rate Limiting

**Tiempo estimado:** 2 horas

```bash
# 1. Agregar dependencia al pom.xml
# (Ver SECURITY_DEPENDENCIES.xml)

# 2. Copiar RateLimitFilter.java
# cp /path/to/RateLimitFilter.java src/main/java/com/rep/security/

# 3. Actualizar SecurityConfig.java para usar RateLimitFilter
```

**Código de referencia:** `RateLimitFilter.java` ya creado

### 2.2 Agregar Validación de Entrada Global

**Tiempo estimado:** 3 horas

Crear validator para todos los DTOs:

```java
// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank(message = "Identificación requerida")
    @Size(min = 5, max = 20, message = "Identificación debe tener entre 5 y 20 caracteres")
    private String identificacion;

    @NotBlank(message = "Contraseña requerida")
    @Size(min = 8, max = 100, message = "Contraseña debe tener al menos 8 caracteres")
    private String password;
}

// UsuarioDTO.java
@Data
public class UsuarioDTO {
    @NotNull
    @Email(message = "Email debe ser válido")
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{8,15}$", message = "Cédula debe contener solo números")
    private String cedula;
}

// Agregar GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
```

### 2.3 Implementar Global Exception Handler

**Tiempo estimado:** 2 horas

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        String trackingId = UUID.randomUUID().toString();
        logger.error("Error ID: {} - {}", trackingId, e.getMessage(), e);

        return ResponseEntity.status(500)
            .body(new ErrorResponse(
                "Error interno del servidor",
                trackingId
            ));
    }
}

// ErrorResponse.java
@Data
public class ErrorResponse {
    private String message;
    private String trackingId;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, String trackingId) {
        this.message = message;
        this.trackingId = trackingId;
        this.timestamp = LocalDateTime.now();
    }
}
```

### 2.4 Reducir Logging en Production ✅

**Tiempo estimado:** 1 hora

Cambiar en todos los controladores de:

```java
System.out.println("=== initialize() ejecutado ===");
System.out.println("Token disponible: " + ...);
```

A:

```java
logger.debug("Component initialized");
// NO loguear tokens o datos sensibles
```

---

## Fase 3: IMPORTANTE (Semana 3-4)

### 3.1 Implementar HTTPS/TLS

**Tiempo estimado:** 3 horas

```bash
# 1. Generar keystore
keytool -genkeypair \
    -alias tomcat \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore /etc/ssl/keystore/tomcat.p12 \
    -validity 365 \
    -storepass $SSL_KEYSTORE_PASSWORD

# 2. Configurar en application-prod.properties
server.ssl.enabled=true
server.ssl.key-store=${SSL_KEYSTORE_PATH}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

### 3.2 Agregar Security Headers Completos

**Tiempo estimado:** 1 hora

```bash
# Usar SecurityConfigSecure.java ya creado
# cp /path/to/SecurityConfigSecure.java src/main/java/com/rep/config/
```

**Headers implementados:**

- HSTS (HTTP Strict Transport Security)
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- Content-Security-Policy
- X-XSS-Protection

### 3.3 Implementar Auditoría Completa

**Tiempo estimado:** 4 horas

```java
// AuditLog.java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue
    private Long id;

    private String usuario;
    private String accion;
    private String recurso;
    private String resultado;  // SUCCESS, FAILURE
    private String detalles;
    @CreationTimestamp
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
}

// AuditService.java
@Service
public class AuditService {
    public void registrarAccion(String usuario, String accion,
                                 String recurso, String resultado) {
        AuditLog log = new AuditLog();
        // Guardar en BD
        auditRepository.save(log);
    }
}

// AuditAspect.java
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        // Registrar antes de ejecutar
        // Registrar resultado
    }
}
```

### 3.4 Encriptar Datos Sensibles

**Tiempo estimado:** 3 horas

```bash
# 1. Agregar dependencia Jasypt
# (Ver SECURITY_DEPENDENCIES.xml)

# 2. Encryptar propiedades sensibles
mvn jasypt:encrypt -Djasypt.maven.plugin.value="valor_a_encriptar" \
                   -Djasypt.maven.plugin.password="jasypt_password"
```

---

## Fase 4: MEJORA CONTINUA (Mensual)

### 4.1 Scanning de Dependencias

**Tiempo estimado:** 30 minutos

```bash
# Verificar CVEs en dependencias
mvn clean dependency-check:check

# Ver reporte
open target/dependency-check-report.html
```

### 4.2 Análisis Estático (SonarQube)

**Tiempo estimado:** 1 hora/semana

```bash
# Iniciar SonarQube (primera vez)
docker run -d --name sonarqube -p 9000:9000 sonarqube:latest

# Analizar proyecto
mvn clean package sonar:sonar \
    -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.login=admin \
    -Dsonar.password=admin
```

### 4.3 Penetration Testing

**Tiempo estimado:** 4 horas

```bash
# Usar OWASP ZAP
docker run -t owasp/zap2docker-stable \
    zap-baseline.py -t http://localhost:8080

# O usar Burp Community
# https://portswigger.net/burp/community
```

### 4.4 Monitoreo Continuo

**Tiempo estimado:** 2 horas configuración inicial

```bash
# Activar Prometheus metrics
# Crear dashboard en Grafana
docker run -d --name grafana -p 3000:3000 grafana/grafana

# Configurar alertas para:
# - Intentos de acceso fallidos
# - Tráfico anómalo
# - Cambios en datos sensibles
```

---

## 📅 Cronograma Recomendado

```
SEMANA 1 (Lunes-Viernes)
├─ Lunes: Fases 1.1-1.3 (Credenciales, JWT, SSL)
├─ Martes: Fase 1.4-1.5 (Remover debug, Logging)
├─ Miércoles-Viernes: Testing y validación Fase 1
└─ Viernes: Deploy a ambiente de staging

SEMANA 2
├─ Lunes-Miércoles: Fase 2.1-2.2 (Rate Limiting, Validación)
├─ Jueves: Fase 2.3-2.4 (Exception Handling, Logging)
└─ Viernes: Testing Fase 2, Deploy staging

SEMANA 3-4
├─ HTTPS/TLS Implementation
├─ Security Headers
├─ Auditoría
├─ Encriptación
└─ Deploy PRODUCCIÓN después de Fase 3

MENSUAL
├─ Dependency scanning
├─ SonarQube analysis
├─ Penetration testing
└─ Revisión de logs y alertas
```

---

## 🧪 Testing Checklist

Para cada fase, verificar:

- [ ] Tests unitarios pasen
- [ ] Tests de integración pasen
- [ ] Tests de seguridad pasen (si aplica)
- [ ] No hay regresiones funcionales
- [ ] Performance no se degrada
- [ ] Logs se ven correctos
- [ ] No hay credenciales expuestas en logs
- [ ] CORS funciona correctamente
- [ ] JWT se valida correctamente
- [ ] Rate limiting bloquea intentos excesvios
- [ ] Validación de entrada rechaza datos malos
- [ ] Excepciones se manejan sin exponer stack traces

---

## 🚀 Deployment Checklist

Antes de cada deploy:

- [ ] Todos los tests pasen
- [ ] Análisis estático sin críticos
- [ ] No hay CVEs en dependencias
- [ ] Credenciales en variables de entorno
- [ ] Base de datos hace backup
- [ ] Plan de rollback documentado
- [ ] Monitoreo activo
- [ ] Logs se archivan correctamente
- [ ] SSL certificate válido
- [ ] CORS configurado para dominios reales
- [ ] Rate limiting habilitado
- [ ] Auditoría configurada

---

## 📞 Contacto y Soporte

Si encuentras problemas durante la implementación:

1. **Revisar logs:** `tail -f logs/application.log`
2. **Verificar configuración:** `env | grep -i db_\|jwt\|ssl`
3. **Testear conectividad:** `nc -zv db.host 3306`
4. **Consultar documentación:**
   - Spring Security: https://spring.io/projects/spring-security
   - OWASP: https://owasp.org/
   - JWT: https://jwt.io/

---

**Documento actualizado:** 25 de enero de 2026
