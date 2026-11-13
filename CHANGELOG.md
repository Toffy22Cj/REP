# Changelog — REP Backend

## [Material y archivos 2026-11-13]
- **Material de estudio**: API `/api/materiales` (publicar, listar según rol, descargar,
  eliminar) sobre la entidad `Recurso`.
- **Archivos**: `AlmacenamientoArchivosService` (lista blanca de tipos, nombres UUID,
  `STORAGE_DIR`, máx. `MAX_FILE_SIZE`=25MB, descarga en línea de imágenes/PDF/audio/vídeo).
  Corrige nombres con tildes en servidores sin locale UTF-8.
- **Preguntas**: las de verdadero/falso no guardaban sus opciones (siempre incorrectas).
  Reglas por tipo (`PREGUNTA_OPCIONES_INVALIDAS`), orden estable, URL de descarga en
  lugar de la ruta interna del servidor.
- **Inicio de sesión**: el bloqueo contaba también los accesos correctos; ahora solo los
  fallidos (`LOGIN_MAX_INTENTOS`, `LOGIN_BLOQUEO_MINUTOS`).
- **Datos de demostración** con `SEED_DEMO=true` (`DemoDataSeeder`).
- Nuevos códigos de error: `ARCHIVO_VACIO`, `ARCHIVO_TIPO_NO_PERMITIDO`,
  `ARCHIVO_NO_ENCONTRADO`, `ARCHIVO_NO_GUARDADO`, `MATERIAL_NO_ENCONTRADO`,
  `PREGUNTA_OPCIONES_INVALIDAS`.
- Pruebas: 60 → 68.


## [Profesionalización 2026-11] — Errores con nombre y gestión académica

### Manejo de errores
- **Catálogo de errores** (`ErrorCode`, documentado en `docs/errores.md`): cada error
  tiene un nombre estable (p. ej. `USUARIO_IDENTIFICACION_DUPLICADA`,
  `DOCUMENTO_LONGITUD_INVALIDA`, `CURSO_CON_ESTUDIANTES`), estado HTTP y mensaje.
- **Formato único `ApiError`** en todas las rutas, incluidos los filtros de seguridad
  (401/403/429). Antes convivían texto plano, mapas ad hoc y cuerpos vacíos: el panel web
  mostraba "Error 400:" sin explicación.
- Excepciones con nombre sobre una base común `RepException`; se eliminan duplicadas
  (`ResourceNotFoundException`, `EntityNotFoundException` propia, `ApiException`) y los
  `catch (Exception)` de los controladores que devolvían 500 con mensajes internos.
- Not-found que lanzaban `RuntimeException` (→ 500) ahora son 404 con código.

### Errores funcionales y de seguridad
- **Escalada de privilegios**: crear una actividad en una materia/curso no asignado creaba
  la asignación automáticamente. Ahora `ACCESO_DENEGADO`.
- Edición de usuario parcial: el cliente de escritorio no enviaba `apellido` y se guardaba
  `null` (error 500).
- El administrador ya no puede desactivarse ni eliminarse a sí mismo.
- No se permite cambiar el rol de profesores/estudiantes (dejaba datos inconsistentes).
- Nuevo `PUT /api/admin/materias/{id}` (el cliente de escritorio ya lo usaba).

### Arquitectura
- `AdminApi` (520 líneas con repositorios y try/catch) pasa a ser un controlador delgado
  sobre `AdminUsuarioService`, `AdminAcademicoService` y `AuditoriaService`.
- Reglas CC/TI centralizadas en `DocumentoIdentidadPolicy`.

### Panel web de administración
- Errores mostrados con su mensaje real y asociados al campo correspondiente.
- Validación previa al envío (mismas reglas que el backend).
- Gestión de **asignaciones profesor-materia-curso** (no existía en el panel web), ver
  estudiantes de un curso, eliminar cursos/materias, activar/desactivar usuarios y ver
  detalles (antes eran `alert("en v2.1")`).

### Pruebas
- 45 → **60** pruebas (`ApiErrorContractTest`: contrato de errores y reglas nuevas).


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
