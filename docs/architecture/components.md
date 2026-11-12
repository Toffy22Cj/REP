# 🧩 Componentes del Sistema REP

Este documento detalla el catálogo completo de componentes, clases, interfaces y servicios que conforman el sistema, organizados por subsistemas funcionales, detallando sus responsabilidades, dependencias e interacciones.

---

## 1. Subsistema de Seguridad y Control de Acceso

| Componente | Tipo | Ubicación | Responsabilidad Principal | Dependencias Clave |
| :--- | :--- | :--- | :--- | :--- |
| `SecurityConfig` | Clase `@Configuration` | `com.rep.config` | Configuración de las 3 cadenas de filtros (`SecurityFilterChain`), CORS, encriptador de contraseñas y headers de seguridad. | `CustomUserDetailsService`, `SecretKey`, `JwtConfig`, `RateLimitFilter` |
| `JwtConfig` | Clase `@Configuration` | `com.rep.config` | Exposición de la clave criptográfica `SecretKey` (HMAC-SHA) y periodo de expiración leídos de `jwt.secret` y `jwt.expiration`. | Ninguna |
| `JwtAuthenticationFilter` | Filtro `OncePerRequestFilter` | `com.rep.security` | Intercepta cabeceras `Authorization: Bearer`, valida la firma JWT, extrae autoridades y establece la autenticación en el `SecurityContext`. | `SecretKey`, `JwtConfig`, `CustomUserDetailsService` |
| `RateLimitFilter` | Filtro `OncePerRequestFilter` | `com.rep.security` | Aplica límites de peticiones por dirección IP utilizando el algoritmo Token Bucket de Bucket4j. | `Bucket4j`, `Bandwidth` |
| `CustomUserDetailsService` | Servicio `@Service` | `com.rep.service.logica` | Implementa `UserDetailsService` de Spring Security para cargar el usuario desde base de datos por su identificación. | `UsuarioRepository` |
| `AuthService` | Servicio `@Service` | `com.rep.service.logica` | Valida credenciales contra `PasswordEncoder`, verifica que la cuenta esté activa y emite tokens JWT. | `UsuarioRepository`, `PasswordEncoder`, `SecretKey`, `JwtConfig` |
| `AuthController` | Controlador `@RestController` | `com.rep.controller.apis` | Expone el endpoint público `/api/auth/login`. | `AuthService` |
| `ValidacionService` | Servicio `@Service` | `com.rep.service.logica` | Verificador de fronteras y aislamiento de acceso: valida que un profesor tenga jurisdicción sobre materias, cursos, actividades, estudiantes y preguntas. | `MateriaService`, `CursoService`, `ActividadService`, `EstudianteService`, `PreguntaService`, `RespuestaService` |

---

## 2. Subsistema de Núcleo Académico (Academic Core)

| Componente | Tipo | Ubicación | Responsabilidad Principal | Dependencias Clave |
| :--- | :--- | :--- | :--- | :--- |
| `Curso` | Entidad `@Entity` | `com.rep.model` | Representa un grado y grupo escolar (ej. 10-A, 11-B). | Lista de `Estudiante` |
| `Materia` | Entidad `@Entity` | `com.rep.model` | Representa una asignatura académica (ej. Matemáticas, Biología). | Ninguna |
| `ProfesorMateria` | Entidad `@Entity` | `com.rep.model` | Entidad pivote ternaria `(Profesor, Materia, Curso)` con restricción de unicidad estricta. | `Profesor`, `Materia`, `Curso` |
| `CursoService` / `CursoServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Lógica de creación, consulta, conteo de estudiantes y validación de cursos duplicados. | `CursoRepository`, `ProfesorMateriaRepository`, `CursoMapper` |
| `MateriaService` / `MateriaServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Consulta de materias asignadas a docentes y validación de acceso. | `MateriaRepository` |
| `ProfesorMateriaService` / `ProfesorMateriaServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Operaciones CRUD y consultas de cruce de asignaciones académicas. | `ProfesorMateriaRepository` |
| `CursoMapper` | Mapeador `@Mapper` | `com.rep.service.mapas` | Interfaz MapStruct que compila transformaciones bidireccionales entre `Curso` y `CursoDTO`. | MapStruct |
| `PublicCursoApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoint público `/api/public/cursos` para consultar cursos disponibles en procesos de registro. | `CursoRepository` |

---

## 3. Subsistema de Evaluación y Actividades Pedagógicas

| Componente | Tipo | Ubicación | Responsabilidad Principal | Dependencias Clave |
| :--- | :--- | :--- | :--- | :--- |
| `Actividad` | Entidad `@Entity` | `com.rep.model` | Define la evaluación (`EXAMEN`, `QUIZ`, `TALLER`), fecha/hora límite, duración y vinculación a `ProfesorMateria`. | `ProfesorMateria`, Set de `Pregunta` |
| `Pregunta` | Entidad `@Entity` | `com.rep.model` | Reactivo evaluativo (`OPCION_MULTIPLE`, `VERDADERO_FALSO`, `RESPUESTA_ABIERTA`) con soporte para URL/tipo de archivo multimedia. | `Actividad`, Set de `Opcion` |
| `Opcion` | Entidad `@Entity` | `com.rep.model` | Alternativa de respuesta con bandera `esCorrecta` y soporte de archivo adjunto / pictograma. | `Pregunta` |
| `RespuestaEstudiante` | Entidad `@Entity` | `com.rep.model` | Intento de entrega de un estudiante con nota calculada (0.0 a 5.0), duración, estado `entregado` y observaciones. | `Estudiante`, `Actividad`, `Profesor`, Lista de `RespuestaPregunta` |
| `RespuestaPregunta` | Entidad `@Entity` | `com.rep.model` | Respuesta específica dada a una pregunta (opción elegida, texto abierto o archivo subido). | `RespuestaEstudiante`, `Estudiante`, `Pregunta`, `Opcion` |
| `ActividadService` / `ActividadServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Gestión del ciclo de vida de actividades, cálculo de métricas de entrega y consolidación de resultados. | `ActividadRepository`, `RespuestaEstudianteRepository`, `ProfesorMateriaRepository` |
| `PreguntaService` / `PreguntaServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Creación/actualización de preguntas, validación de opción única correcta, límite de 5 opciones y subida física de archivos. | `PreguntaRepository`, `OpcionRepository`, `ActividadService`, `RespuestaPreguntaRepository` |
| `CorreccionService` | Servicio `@Service` | `com.rep.service.logica` | Motor puro de evaluación de opciones y generación de retroalimentación explicativa al estudiante. | Ninguna (stateless) |
| `ActividadApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoints REST `/api/actividades/**` para docentes y estudiantes. | `ActividadService`, `ValidacionService`, `EstudianteService`, `ProfesorMateriaRepository` |
| `PreguntaApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoints REST `/api/preguntas/**` para gestión de reactivos, subida y descarga autorizada de archivos. | `PreguntaService`, `EstudianteService` |

---

## 4. Subsistema de Estudiantes y Profesores

| Componente | Tipo | Ubicación | Responsabilidad Principal | Dependencias Clave |
| :--- | :--- | :--- | :--- | :--- |
| `Usuario` | Entidad base `@Entity` | `com.rep.model` | Raíz de herencia `JOINED`. Modela nombre, apellido, correo, identificación, tipo doc, contraseña, rol (`ADMIN`, `PROFESOR`, `ESTUDIANTE`), activo y auditoría. | Implementa `UserDetails` |
| `Estudiante` | Entidad `@Entity` | `com.rep.model` | Extensión de `Usuario`. Asocia curso actual, edad y estado (`activo`, `retirado`, `graduado`, `suspendido`). | Extiende `Usuario`, `@ManyToOne Curso` |
| `Profesor` | Entidad `@Entity` | `com.rep.model` | Extensión de `Usuario`. Modela edad docente, fecha de ingreso y estado (`activo`, `retirado`). | Extiende `Usuario` |
| `EstudianteService` / `EstudianteServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Orquesta la resolución de actividades, validaciones de deadline/intentos, cálculo preliminar de nota y exportación CSV de listas. | Repositorios de Estudiante, Actividad, Respuesta, Opciones y `CorreccionService` |
| `UsuarioRegistrationService` | Servicio `@Service` | `com.rep.service.logica` | Servicio transaccional que valida unicidad de documento/correo y crea instancias concretas (`Estudiante` o `Profesor`). | `UsuarioRepository`, `EstudianteRepository`, `ProfesorRepository`, `PasswordEncoder`, `CursoRepository` |
| `UsuarioMigrationService` | Servicio `@Service` | `com.rep.service.logica` | Servicio utilitario para normalizar registros legados de usuarios asignándoles un curso temporal `(0-TEMP)` o definitivo `(1-A)`. | Repositorios de Usuario, Estudiante, Profesor y Curso |
| `EstudianteApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoints `/api/estudiante/**` para consultar materias asignadas, actividades disponibles, enviar soluciones y consultar resultados. | `NotificacionService`, `EstudianteService` |
| `ProfesorApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoints `/api/profesor/**` para docentes: asignaturas a cargo, grupos, respuestas recibidas, calificación manual y estadísticas. | Múltiples servicios de dominio y `ValidacionService` |
| `RegistroApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoint público `/api/registro` para auto-registro de usuarios. | `UsuarioRegistrationService` |

---

## 5. Subsistema de Asistencias y Reportes

| Componente | Tipo | Ubicación | Responsabilidad Principal | Dependencias Clave |
| :--- | :--- | :--- | :--- | :--- |
| `Asistencia` | Entidad `@Entity` | `com.rep.model` | Registro de presencia diaria (`PRESENTE`, `NO_INGRESO`, `EXCUSA`) vinculado a una fecha, estudiante y `ProfesorMateria`. Restricción única `(profesor_materia_id, estudiante_id, fecha)`. | `Estudiante`, `ProfesorMateria` |
| `AsistenciaService` / `AsistenciaServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Consulta y guardado masivo de asistencias por curso, asignatura y fecha. | `AsistenciaRepository`, `ProfesorMateriaRepository`, `EstudianteRepository` |
| `ExcelReportGenerator` | Utilidad de reporte | `com.rep.service.archivos` | Genera libros binarios de Microsoft Excel (`.xlsx`) con estilos de encabezado y auto-ajuste de columnas. | Apache POI (`XSSFWorkbook`, `Sheet`, `CellStyle`) |
| `PdfReportGenerator` | Utilidad de reporte | `com.rep.service.archivos` | Genera documentos PDF con títulos y tablas formateadas con resultados de estudiantes. | iText Core 7 (`com.itextpdf.kernel.*`, `com.itextpdf.layout.*`) |
| `RespuestaService` / `RespuestaServiceImpl` | Interfaz y Servicio | `com.rep.service.logica/impl` | Coordinación de reportes de promedios, filtrado avanzado de entregas y generación binaria PDF/Excel. | `RespuestaEstudianteRepository`, `RespuestaPreguntaRepository`, `ActividadRepository`, `ExcelReportGenerator`, `PdfReportGenerator` |

---

## 6. Subsistema de Administración y Operaciones del Sistema

| Componente | Tipo | Ubicación | Responsabilidad Principal | Dependencias Clave |
| :--- | :--- | :--- | :--- | :--- |
| `AuditLog` | Entidad `@Entity` | `com.rep.model` | Bitácora inmutable de eventos administrativos (`username`, `action`, `details`, `timestamp`). | `@EntityListeners(AuditingEntityListener.class)` |
| `AdminApi` | Controlador `@RestController` | `com.rep.controller.apis` | Centro de comando administrativo: CRUD de cursos, materias, usuarios, asignaciones docentes, transferencias de alumnos y auditoría. | Repositorios de Usuario, Estudiante, Profesor, Curso, Materia, ProfesorMateria, AuditLog y `PasswordEncoder` |
| `SystemApi` | Controlador `@RestController` | `com.rep.controller.apis` | Endpoints de control de infraestructura: métricas de memoria/uptime (`/api/admin/system/status`) y apagado ordenado asíncrono (`/api/admin/system/shutdown`). | `ApplicationContext`, `SpringApplication` |
| `DataLoader` | Componente `@Component` | `com.rep.config` | `CommandLineRunner` que verifica al arranque si existe el usuario administrador inicial (identificación `"22"`). Si no existe, lo crea con contraseña codificada BCrypt. | `UsuarioRepository`, `PasswordEncoder` |
| `BrowserLauncher` | Componente `@Component` | `com.rep.config` | Escucha `ApplicationReadyEvent` e intenta abrir el navegador web del sistema operativo apuntando a `http://localhost:8080/admin/`, omitiendo si detecta modo headless. | `Desktop`, `Runtime` |
| `WebConfig` | Clase `@Configuration` | `com.rep.config` | Registra redirecciones y reenvíos MVC para la interfaz administrativa estática (`/admin/` -> `/admin/index.html`). | `WebMvcConfigurer`, `ViewControllerRegistry` |

---

## 7. Matriz de Dependencias entre Componentes

```
┌─────────────────────┬────────────────────────────────────────────────────────────────────────────────────────┐
│ Componente          │ Componentes de los que Depende Directamente                                            │
├─────────────────────┼────────────────────────────────────────────────────────────────────────────────────────┤
│ SecurityConfig      │ CustomUserDetailsService, JwtConfig, RateLimitFilter, JwtAuthenticationFilter           │
│ AdminApi            │ UsuarioRepository, EstudianteRepository, ProfesorRepository, CursoRepository,         │
│                     │ MateriaRepository, ProfesorMateriaRepository, AuditLogRepository, PasswordEncoder      │
│ ActividadApi        │ ActividadService, ValidacionService, EstudianteService, ProfesorMateriaRepository     │
│ ProfesorApi         │ MateriaService, CursoService, ActividadService, PreguntaService, EstudianteService,    │
│                     │ RespuestaService, ValidacionService, AsistenciaService                                 │
│ EstudianteApi       │ NotificacionService, EstudianteService                                                 │
│ PreguntaApi         │ PreguntaService, EstudianteService                                                     │
│ ValidacionService   │ MateriaService, CursoService, ActividadService, EstudianteService, PreguntaService,     │
│                     │ RespuestaService, ProfesorMateriaRepository, PreguntaRepository                        │
│ EstudianteServiceImpl│ EstudianteRepo, ActividadRepo, MateriaRepo, RespuestaEstudianteRepo, PreguntaRepo,      │
│                     │ OpcionRepo, RespuestaPreguntaRepo, CorreccionService, CalificacionRepo                 │
│ ActividadServiceImpl│ ActividadRepo, RespuestaEstudianteRepo, ProfesorMateriaRepo                            │
│ PreguntaServiceImpl │ PreguntaRepo, OpcionRepo, ActividadService, RespuestaPreguntaRepo                      │
│ RespuestaServiceImpl│ RespuestaEstudianteRepo, RespuestaPreguntaRepo, ActividadRepo,                         │
│                     │ ExcelReportGenerator, PdfReportGenerator                                               │
└─────────────────────┴────────────────────────────────────────────────────────────────────────────────────────┘
```
