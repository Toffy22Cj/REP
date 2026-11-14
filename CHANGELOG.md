# Changelog — REP Backend

## [Horario, mDNS y app móvil 2026-11-14]
- **Horario de clases** (`BloqueHorario`, `com.rep.horario`): el administrador crea las
  clases por asignación, día, hora y aula (`/api/admin/horario`); se impiden los cruces
  del curso (`HORARIO_CRUCE_CURSO`) y del profesor (`HORARIO_CRUCE_PROFESOR`). Consulta:
  `GET /api/estudiante/{id}/horario` y `GET /api/profesor/horario`. Al borrar una
  asignación se borran sus bloques. Datos demo con horario semanal.
- **mDNS/DNS-SD**: el servidor se anuncia como `_rep._tcp.local.` (JmDNS) junto al
  descubrimiento UDP; se ve en Panel › Red. Variable `REP_MDNS`.
- Nuevos códigos: `HORARIO_BLOQUE_NO_ENCONTRADO`, `HORARIO_HORAS_INVALIDAS`,
  `HORARIO_CRUCE_CURSO`, `HORARIO_CRUCE_PROFESOR`. Pruebas: 96 → 98.

## [Red, primer acceso e instalación 2026-11-14]
- **Descubrimiento en red** (`com.rep.red`): el servidor responde búsquedas UDP (47800)
  por broadcast, multicast y localhost y se anuncia cada 5 s (47801) en todas sus redes
  (cable, Wi-Fi, red propia, Bluetooth PAN). Identidad permanente (`servidor.id`) para
  que los clientes lo sigan si cambia de IP. `GET /api/public/servidor`.
- **Panel web › Red**: nombre del servidor, activar/desactivar el descubrimiento,
  direcciones por interfaz (con tipo), equipos que lo buscaron y red Wi-Fi propia
  (`nmcli` en Linux; instrucciones en Windows/macOS). `GET/PUT /api/admin/red`,
  `POST/DELETE /api/admin/red/hotspot`. Se retira el enlace "Archivos" (estaba vacío).
- **Primer acceso**: la contraseña generada del administrador es provisional; la página
  de acceso la muestra una sola vez y solo en el propio servidor; hasta cambiarla la API
  responde `CAMBIO_CONTRASENA_OBLIGATORIO` (web y cliente). Si no se cambió, se regenera
  en cada arranque.
- **Instalación preconfigurada** (perfil `local`): base H2 embebida en `REP_HOME`,
  secreto JWT generado y guardado automáticamente (ya no es obligatorio `JWT_SECRET`),
  registros y archivos en `REP_HOME`.
- Empaquetado: `empaquetado/servidor-linux.sh` (jpackage), iconos y LEEME.
- Versión 1.0.0. Nuevos códigos: `CAMBIO_CONTRASENA_OBLIGATORIO`, `HOTSPOT_NO_SOPORTADO`,
  `HOTSPOT_FALLO`. Guía `docs/INSTALACION_Y_RED.md`. Pruebas: 92 → 96.

## [Resultados y entregas 2026-11-14]
- **Mostrar u ocultar respuestas correctas**: nuevo campo `mostrarRespuestas` en la
  actividad (por defecto `true`, como antes). Con `false`, el resultado del estudiante
  solo trae nota y observaciones (`respuestasVisibles=false`).
- El resultado por pregunta incluye `enunciado`, `respuestaEstudiante` y
  `respuestaCorrecta`.
- La lista de actividades del estudiante incluye `estadoEntrega` (PENDIENTE, EN_CURSO,
  ENTREGADA, VENCIDA) y su `nota`: antes seguía ofreciendo "Resolver" en actividades
  ya entregadas y el servidor negaba el acceso.
- Pruebas: 90 → 92.

## [Madurez: periodos, migraciones y CI 2026-11-13]
- **Flyway**: migraciones versionadas con baseline para bases existentes. La migración
  `V2__Enums_a_varchar` convierte las columnas `ENUM` nativas (creadas por Hibernate en
  MySQL) a `VARCHAR`; sin ella, añadir valores a un enumerado rompía los INSERT. Las
  entidades usan `@JdbcTypeCode(SqlTypes.VARCHAR)`. Variables `FLYWAY_ENABLED`, `DDL_AUTO`.
- **Periodos académicos** (`PeriodoAcademico`): `/api/periodos` y `/api/admin/periodos`,
  validación de fechas, solapes, números y porcentajes; notas por periodo
  (`periodoId` en boletín, planilla y exportaciones) y nota anual ponderada cuando hay
  periodos (sin periodos el cálculo no cambia). PDF con desglose por periodo.
- **Asistencia**: estado `RETARDO` (cuenta como asistencia, se informa en el boletín). Un
  estado desconocido ya no se guarda en silencio como falla: responde `CAMPOS_INVALIDOS`.
- La actividad con preguntas incluye `duracionMinutos` y `fechaEntrega` (temporizador
  del cliente).
- Datos demo con 4 periodos de 25 %.
- CI en GitHub Actions: pruebas con Maven y arranque real contra MySQL 8.
- Nuevos códigos: `PERIODO_NO_ENCONTRADO`, `PERIODO_FECHAS_INVALIDAS`, `PERIODO_SOLAPADO`,
  `PERIODO_DUPLICADO`, `PERIODO_PORCENTAJE_EXCEDIDO`.
- Informe `docs/PRODUCT_EVOLUTION_AND_MATURITY_REPORT.md` (con fuentes) y
  `docs/migraciones.md`.
- Pruebas: 83 → 90.

## [Perfil, notas y gestión 2026-11-13]
- **Mi perfil** para todos los roles: `GET/PUT /api/perfil` (y `/api/admin/perfil` para el
  panel web) y `PUT /api/perfil/contrasena`. Cambiar la contraseña invalida los tokens
  emitidos antes (`credenciales_actualizadas_en`) y devuelve uno nuevo.
- **Notas**: boletín del estudiante (`GET /api/estudiante/{id}/notas`, PDF en `/notas/pdf`)
  y planilla del profesor (`GET /api/profesor/notas`, exportación `xlsx`/`pdf`), con
  desempeño según el Decreto 1290. Configurable con `NOTA_MINIMA_APROBATORIA` (3.0) y
  `VENCIDAS_CUENTAN_CERO` (true).
- `GET /api/profesor/asignaciones` y `GET /api/profesor/actividades/{id}/respuestas`
  (el cliente pedía esta ruta y no existía: la pantalla de calificar quedaba vacía).
- `PUT /api/actividades/{id}/estado?activa=` para publicar u ocultar una actividad;
  `ActividadDTO` incluye `activa`.
- **Corregido**: la copia de una actividad no quedaba asociada al profesor (no aparecía en
  su lista ni se podía editar); editar no guardaba la duración; las estadísticas contaban
  intentos sin entregar como entregas y dividían por cero en cursos vacíos (ahora además
  dan promedio, máximo, mínimo, revisadas, en curso y sin iniciar); la respuesta de un
  estudiante exponía la ruta interna del archivo.
- El auto-registro crea la cuenta inactiva hasta que la administración la activa; el
  mensaje de cuenta inactiva lo explica.
- **Rendimiento en redes lentas**: compresión gzip, `ETag`/304 en descargas, índices en
  `respuesta_estudiante`, consultas agrupadas en notas y asistencia; auditoría limitada.
- Nuevos códigos: `CONTRASENA_ACTUAL_INCORRECTA`, `CONTRASENA_DEBIL`, `CONTRASENA_REPETIDA`.
- Panel web: sección **Mi perfil** (datos y contraseña) y nombre del administrador en la
  barra superior.
- Pruebas: 68 → 83.

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
