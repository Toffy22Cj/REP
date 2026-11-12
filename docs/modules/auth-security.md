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
- **Sesiones:** `STATELESS`. Peticiones sin autenticar responden **401** (no redirección).
- **Filtros:** `RateLimitFilter` y `JwtAuthenticationFilter` (solo dentro de las cadenas; ya no se registran además como filtros globales del contenedor).
- **Cabeceras:** `Content-Security-Policy: default-src 'self'`, `X-Frame-Options: DENY`.
- **Reglas de acceso:**
  - `/api/public/**`, `/api/auth/**`, `/api/registro`: públicos.
  - `GET /api/preguntas/*/archivo`, `GET /api/preguntas/opciones/*/archivo`, `GET /api/actividades/*/preguntas`: `ESTUDIANTE`, `PROFESOR`, `ADMIN` (el controlador valida la pertenencia).
  - `/api/estudiante/**`: `ESTUDIANTE`, `ADMIN`. Además el controlador exige que el id del path sea el del usuario autenticado (o admin); **solo el propio estudiante puede entregar** una actividad.
  - `/api/profesor/**`: `PROFESOR`.
  - `/api/actividades/**`, `/api/preguntas/**`: `PROFESOR`, `ADMIN`, con validación de propiedad en `ValidacionService`.

### 3.2 Cadena de Administración (`adminSecurityFilterChain` - Orden 2)
- **Matcher:** `/admin/**`, `/api/admin/**`. Todo requiere `ADMIN` salvo `login.html`, `css/` y `js/`.
- **Dos mecanismos:** sesión con formulario para el panel web y **JWT** para el cliente de escritorio.
- `/api/admin/**` sin autenticar → **401**; `/admin/**` → redirección a `/admin/login.html`.
- Cookie de sesión `HttpOnly` + `SameSite=Strict` (protección CSRF del panel), cambio de id de sesión al iniciar sesión.

### 3.3 Registro público (`POST /api/registro`)
Solo crea cuentas de **ESTUDIANTE** (403 para otros roles). Profesores y administradores se crean desde `POST /api/admin/usuarios`, que construye el subtipo correcto (`Profesor`, `Estudiante` con curso obligatorio, `Usuario` para ADMIN) mediante `UsuarioRegistrationService.construirUsuario`.

---

## 4. Rate Limiting (Bucket4j)

| Ámbito | Límite por IP | Propiedad |
|--------|---------------|-----------|
| `POST /api/auth/login` y `POST /admin/login` | 5 cada 15 min | `app.security.rate-limit.login-per-15min` |
| Resto de `/api/**` | 100 por minuto | `app.security.rate-limit.api-per-minute` |

- **IP del cliente:** `request.getRemoteAddr()`. Las cabeceras `X-Forwarded-For` / `X-Real-IP` solo se usan con `app.security.trust-proxy-headers=true` (variable `TRUST_PROXY_HEADERS`), es decir, detrás de un proxy inverso de confianza; de lo contrario un atacante podría falsificarlas para evadir el límite.
- Memoria acotada: máximo 10.000 IPs registradas.
- Exceso: `429` con cuerpo JSON `{"status":429,"error":"Too Many Requests","message":...}`.

---

## 5. Ciclo de vida del token JWT

### 5.1 Emisión (`AuthService`)
1. `POST /api/auth/login` con `identificacion` y `password`.
2. Usuario inexistente o contraseña incorrecta → **401** (`LoginResponse` con `success=false`). Cuenta inactiva → **403**.
3. Token: subject = identificación, claims `userId` y `authorities`, expiración `jwt.expiration` (24 h por defecto), firma HMAC-SHA con `jwt.secret`.

### 5.2 Validación (`JwtAuthenticationFilter`)
1. Sin cabecera `Bearer` → se continúa sin autenticar.
2. Firma/expiración inválidas → **401** JSON.
3. Se carga el usuario de la BD; si está desactivado → **401**.
4. **Las autoridades se toman de la BD, no de la claim `authorities`**: un cambio de rol o una desactivación surten efecto inmediato y un token con rol manipulado no concede privilegios.

### 5.3 Secretos y arranque
- `jwt.secret` = variable `JWT_SECRET`, **obligatoria** y de al menos 32 bytes (la aplicación no arranca en caso contrario, con un mensaje explícito). Generar con `openssl rand -base64 64`.
- Credenciales de BD: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`. Plantilla en `.env.example`.
- Administrador inicial (identificación `22`): contraseña en `ADMIN_INITIAL_PASSWORD`; si no se define se genera una aleatoria y se muestra **una vez** en el log.

---

## 6. Aislamiento Lógico de Dominio (`ValidacionService`)

Para complementar a Spring Security, [ValidacionService.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/logica/ValidacionService.java) actúa como una barrera de negocio inquebrantable:

- `validarProfesorMateria(profesorId, materiaId)`: Comprueba si existe la asignación en `materiaRepository.existsByProfesorIdAndMateriaId`.
- `validarProfesorCurso(profesorId, cursoId)`: Comprueba en `cursoService.profesorTieneAccesoACurso`.
- `validarProfesorActividad(profesorId, actividadId)`: Valida la existencia de la actividad y que `actividad.profesorMateria.profesor.id == profesorId`.
- `validarProfesorEstudiante(profesorId, estudianteId)`: Valida que el estudiante pertenezca a un curso donde el docente dicta alguna materia.
- `validarProfesorPregunta(profesorId, preguntaId)`: Valida que la pregunta pertenezca a una actividad propia del docente.

Si cualquiera de estas comprobaciones falla, se arroja `com.rep.exception.AccessDeniedException`, capturada por el `GlobalExceptionHandler` retornando un código HTTP `403 FORBIDDEN`.
