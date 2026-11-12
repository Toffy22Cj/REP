# 🔐 Módulo de Autenticación, Seguridad y Control de Acceso

## 1. Propósito y Alcance

El módulo de seguridad del sistema REP implementa una estrategia de defensa en profundidad compuesta por tres anillos de protección:
1. **Perímetro de Red y Tráfico (Rate Limiting):** Atenuación y mitigación de ataques de denegación de servicio (DoS) y fuerza bruta contra credenciales mediante el algoritmo Token Bucket.
2. **Perímetro de Sesión y Autenticación Criptográfica:** Emisión y validación sin estado de JSON Web Tokens (JJWT) con clave secreta HMAC-SHA256 para la API REST, en paralelo con una cadena basada en sesiones web y formularios para el panel de administración.
3. **Autorización Granular de Dominio (Domain-Level Authorization):** Mecanismo de validación lógica de jurisdicción docente (`ValidacionService`), asegurando que ningún usuario autenticado acceda a recursos educativos fuera de su ámbito asignado.

---

## 2. Componentes y Clases del Módulo

```
src/main/java/com/rep/
├── config/
│   ├── JwtConfig.java               # Bean SecretKey y tiempo de vida
│   └── SecurityConfig.java          # Configuración de los 3 SecurityFilterChain
├── controller/apis/
│   └── AuthController.java          # Endpoint público POST /api/auth/login
├── dto/auth/
│   ├── AuthUsuarioDTO.java          # Proyección pública del usuario autenticado
│   ├── LoginRequest.java            # Payload de entrada {identificacion, password}
│   └── LoginResponse.java           # Envelope de respuesta {success, token, usuario, mensaje}
├── security/
│   ├── JwtAuthenticationFilter.java # Filtro Bearer Token -> SecurityContext
│   └── RateLimitFilter.java         # Filtro Bucket4j IP Rate Limiter
└── service/logica/
    ├── AuthService.java             # Lógica de emisión y verificación de contraseñas
    ├── CustomUserDetailsService.java# Adaptador Spring Security UserDetailsService
    └── ValidacionService.java       # Validador de pertenencia y reglas de aislamiento
```

---

## 3. Arquitectura de Cadenas de Filtros (`SecurityFilterChain`)

En [SecurityConfig.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/config/SecurityConfig.java), Spring Security 6.2 configura **tres cadenas de filtros ordenadas**:

```
                              Petición HTTP Entrante
                                        │
                                        ▼
                  ¿Coincide con /api/auth/**, /api/public/**,
                  /api/estudiante/**, /api/profesor/**, etc.?
                                   /         \
                             SÍ  /             \  NO
                                /               \
    ┌──────────────────────────▼────┐    ┌───────▼────────────────────────┐
    │  Order(1): apiSecurityFilterChain │    │ ¿Coincide con /admin/**,       │
    │  • SessionPolicy: STATELESS    │    │  /api/admin/**?                │
    │  • RateLimitFilter             │    └───────┬────────────────────────┘
    │  • JwtAuthenticationFilter     │            │
    │  • Headers: CSP, Frame deny    │     SÍ    / \   NO
    │  • Rutas públicas vs con rol   │          /   \
    └───────────────────────────────┘         /     \
                       ┌─────────────────────▼──┐  ┌─▼─────────────────────────┐
                       │ Order(2): adminWebSecurity │  │ Order(3): defaultSecurity      │
                       │ • SessionPolicy: IF_REQUIRED│  │ • Recursos estáticos (css,js) │
                       │ • Form Login: /admin/login │  │ • Raíz /, favicon.ico, error   │
                       │ • Authority: ADMIN          │  └───────────────────────────┘
                       └────────────────────────────┘
```

### 3.1 Cadena de API REST (`apiSecurityFilterChain` - Orden 1)
- **Matcher:** `/api/auth/**`, `/api/public/**`, `/api/estudiante/**`, `/api/profesor/**`, `/api/actividades/**`, `/api/preguntas/**`, `/api/registro`.
- **Sesiones:** `SessionCreationPolicy.STATELESS` (no se crean ni consumen sesiones `JSESSIONID`).
- **Filtros inyectados:**
  1. `RateLimitFilter` antes de `UsernamePasswordAuthenticationFilter`.
  2. `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
- **Cabeceras de Seguridad activadas:**
  - `Content-Security-Policy: default-src 'self'`
  - `X-Frame-Options: DENY`
  - `XSS Protection` habilitada con valores predeterminados.
- **Reglas de Acceso:**
  - `GET /api/preguntas/*/archivo`: Acceso para `ESTUDIANTE`, `PROFESOR`, `ADMIN`.
  - `/api/public/**`, `/api/auth/**`, `/api/registro`: Acceso público (`permitAll()`).
  - `/api/profesor/**`: Requiere autoridad `PROFESOR`.
  - `/api/admin/**`: Requiere autoridad `ADMIN`.
  - `GET /api/actividades/*/preguntas`: Acceso para `ESTUDIANTE`, `PROFESOR`, `ADMIN`, `ROLE_ESTUDIANTE`.
  - `POST /api/estudiante/*/actividades/*/resolver`: Acceso para `ESTUDIANTE`, `ROLE_ESTUDIANTE`.
  - `/api/actividades/**`, `/api/preguntas/**`: Acceso para `PROFESOR`, `ADMIN`.

### 3.2 Cadena Web de Administración (`adminWebSecurityFilterChain` - Orden 2)
- **Matcher:** `/admin/**`, `/api/admin/**`.
- **Sesiones:** `SessionCreationPolicy.IF_REQUIRED`.
- **Mecanismo:** `formLogin()` con página personalizada `/admin/login.html`, punto de procesamiento `/admin/login`, campos `identificacion` y `password`, y redirección predeterminada a `/admin/index.html`.
- **Cierre de sesión:** `logoutUrl("/admin/logout")`, redirección a `/admin/login.html` y purga de cookie `JSESSIONID`.

---

## 4. Algoritmo de Rate Limiting (Bucket4j)

En [RateLimitFilter.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/security/RateLimitFilter.java):

```java
// Límite de inicio de sesión: 5 tokens, recarga de 5 tokens cada 15 minutos
Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));

// Límite general de API: 100 tokens, recarga de 100 tokens cada 1 minuto
Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
```

### Funcionamiento Operativo
1. **Extracción de IP Real:** Se evalúa de manera prioritaria la cabecera `X-Forwarded-For` (tomando la primera IP de la cadena si existen múltiples balanceadores/proxies inversos). Si está ausente, se evalúa `X-Real-IP`, y finalmente `request.getRemoteAddr()`.
2. **Almacenamiento Concurrente:** Los cubos de tokens se mantienen en memoria viva dentro de instancias `ConcurrentHashMap<String, Bucket>`.
3. **Respuesta ante Exceso:** Si se agotan los tokens, la petición se interrumpe retornando estado HTTP `429 TOO MANY REQUESTS`, tipo de contenido `application/json` y el cuerpo `{"error": "Demasiados intentos. Intente más tarde."}`.

---

## 5. Ciclo de Vida del Token JWT

### 5.1 Generación del Token ([AuthService.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/logica/AuthService.java))
1. El usuario envía identificación y contraseña en claro en `POST /api/auth/login`.
2. Se busca al usuario por `identificacion`. Si no existe, se lanza `AuthenticationException("Credenciales inválidas")`.
3. Se verifica el hash mediante `passwordEncoder.matches(password, usuario.getContraseña())`. Si no coincide, se lanza `AuthenticationException`.
4. Se verifica `usuario.isActivo()`. Si está inactivo, se lanza `AccountInactiveException("Cuenta inactiva")`.
5. Se construye el token JWT:
   - **Subject:** `identificacion` del usuario.
   - **Claim `"userId"`:** Clave primaria numérica (`Long id`).
   - **Claim `"authorities"`:** Lista que contiene el nombre del rol (`Collections.singletonList(usuario.getRol().name())`).
   - **IssuedAt:** Marca de tiempo actual.
   - **Expiration:** Fecha actual + `jwt.expiration` (86,400,000 ms = 24 horas por defecto).
   - **Firma:** Algoritmo HMAC-SHA firmado con `SecretKey` (derivada de `jwt.secret`).

### 5.2 Validación e Inyección en Contexto ([JwtAuthenticationFilter.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/security/JwtAuthenticationFilter.java))
1. Se extrae la cabecera `Authorization`. Si no existe o no inicia con `"Bearer "`, se omite el filtro y prosigue la cadena.
2. Se parsea el token con `Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token)`.
3. Se extrae el subject (`username`/identificación) y la lista de autoridades del claim `"authorities"`.
4. **Carga desde Base de Datos:** Se invoca obligatoriamente `userDetailsService.loadUserByUsername(username)` para garantizar que el usuario no haya sido eliminado o desactivado con posterioridad a la emisión del token.
5. Se crea una instancia `UsernamePasswordAuthenticationToken(userDetails, null, authorities)` y se almacena en el hilo de ejecución mediante `SecurityContextHolder.getContext().setAuthentication(authentication)`.

---

## 6. Aislamiento Lógico de Dominio (`ValidacionService`)

Para complementar a Spring Security, [ValidacionService.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/logica/ValidacionService.java) actúa como una barrera de negocio inquebrantable:

- `validarProfesorMateria(profesorId, materiaId)`: Comprueba si existe la asignación en `materiaRepository.existsByProfesorIdAndMateriaId`.
- `validarProfesorCurso(profesorId, cursoId)`: Comprueba en `cursoService.profesorTieneAccesoACurso`.
- `validarProfesorActividad(profesorId, actividadId)`: Valida la existencia de la actividad y que `actividad.profesorMateria.profesor.id == profesorId`.
- `validarProfesorEstudiante(profesorId, estudianteId)`: Valida que el estudiante pertenezca a un curso donde el docente dicta alguna materia.
- `validarProfesorPregunta(profesorId, preguntaId)`: Valida que la pregunta pertenezca a una actividad propia del docente.

Si cualquiera de estas comprobaciones falla, se arroja `com.rep.exception.AccessDeniedException`, capturada por el `GlobalExceptionHandler` retornando un código HTTP `403 FORBIDDEN`.
