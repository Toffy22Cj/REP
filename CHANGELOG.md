# Changelog — REP Backend

## [Refactorización 2026-11] — Seguridad, integridad y modernización

### Críticos (seguridad)
| Hallazgo | Archivo | Corrección |
|---|---|---|
| Cualquiera podía registrarse como **ADMIN** (`POST /api/registro` aceptaba el rol) | `UsuarioRegistrationService` | Solo rol ESTUDIANTE (403 en otro caso); respuesta con DTO |
| **IDOR**: un usuario podía leer o entregar actividades de cualquier estudiante | `EstudianteApi` | Se exige que el id del path sea el del usuario (o admin); entregar solo el propio |
| `@PreAuthorize` sin efecto (method security deshabilitada, validadores `void`): cualquier profesor podía editar/borrar preguntas ajenas | `PreguntaApi` | Validación explícita con `ValidacionService`; la opción debe pertenecer a la pregunta |
| Estudiantes recibían `esCorrecta` (las respuestas) y rutas internas de archivos | `ActividadApi`, `ActividadConPreguntasDTO` | `ocultarSoluciones()` para el rol ESTUDIANTE |
| Exportación de resultados sin control de propiedad | `ProfesorApi` | Validación de actividad/curso y formato |
| Autoridades del JWT tomadas del token; cuentas desactivadas seguían operando 24 h | `JwtAuthenticationFilter` | Autoridades y estado desde la BD |
| Secretos versionados (`.env.local`, `application.properties` raíz, logs) | raíz | Eliminados del repositorio; `.env.example`; `JWT_SECRET` obligatorio |
| XSS almacenado en el panel web (nombres insertados con `innerHTML`) | `static/admin/js` | `escapeHtml` en todas las salidas de datos |

> ⚠️ **Acción requerida:** los secretos que estuvieron en el historial de Git
> (`JWT_SECRET`, credenciales de BD) deben considerarse comprometidos y **rotarse**.

### Altos
- `/api/admin/**` no aceptaba el JWT del cliente de escritorio (redirigía al login web).
- Rate limit evadible falsificando `X-Forwarded-For`; login del panel web sin límite.
- Admin inicial con contraseña fija `admin` (y en realidad nunca se creaba: faltaba `apellido`).
- **Notas incorrectas**: cada respuesta se añadía dos veces a la entrega (los aciertos
  contaban doble) y los reintentos acumulaban respuestas anteriores.
- Login con credenciales incorrectas devolvía 500 (ahora 401; cuenta inactiva 403).
- Alta de usuarios desde administración creaba un `Usuario` genérico: los profesores no
  podían recibir asignaciones. Ahora se crea el subtipo correcto.
- Calificación manual sin validar la escala 0–5; no se registraba quién calificaba.
- Error 500 al serializar entidades con proxies perezosos (calificar respuestas).

### Medios
- Dashboard del profesor: claves que el cliente esperaba (`totalEstudiantes`,
  `actividadesActivas`, `entregasPendientes`) no se enviaban.
- `toString()` de entidades provocaba `LazyInitializationException` y volcaba el hash
  de la contraseña.
- Código muerto del antiguo cliente JavaFX (servicios que llamaban a la propia API por HTTP).
- `@CrossOrigin` dispersos sustituidos por la configuración CORS central
  (`ALLOWED_ORIGINS`).

### Dependencias
- Spring Boot 3.2.0 → **3.2.12** (Spring Security 6.2.8, Tomcat 10.1.33, Logback 1.4.14).
- Eliminados overrides que forzaban versiones antiguas (logback 1.4.7 — CVE-2023-6378 —,
  slf4j, spring-security, jakarta-*), y dependencias sin uso: Spring Cloud OpenFeign, Jasypt.
- Apache POI 5.2.3 → 5.2.5; Lombok 1.18.30 → 1.18.34.

### Configuración
- Logging de producción en INFO, `show-sql` desactivado, actuator limitado a `health`,
  errores sin trazas ni mensajes internos. Perfil `dev` para el modo detallado.

### Pruebas
- 19 → **45** pruebas. Nuevas: `SecurityRegressionTest` (22 casos que reproducen cada
  ataque/defecto), `AdminUsuarioApiTest` (3), escala de notas en `ActividadServiceTest`.
