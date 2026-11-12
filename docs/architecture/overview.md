# 🏛️ Arquitectura del Sistema REP

## 1. Visión Arquitectónica General

El sistema **REP** está concebido bajo un estilo arquitectónico de **Monolito Modular en Capas (Layered Monolith)**, alojado en un único ejecutable autónomo Spring Boot empacado en formato JAR (`main-0.0.1-SNAPSHOT.jar`).

El diseño arquitectónico persigue tres cualidades clave del software:
1. **Alta Cohesión Funcional:** Cada capa y módulo de negocio agrupa operaciones que cambian por las mismas razones (Académico, Evaluación, Seguridad, Reportes).
2. **Desacoplamiento Estricto por Capas:** La capa web (Controllers) delega incondicionalmente a los servicios de dominio (`logica/` e `impl/`), y estos a su vez operan sobre interfaces de persistencia (`repositories/`).
3. **Seguridad Basada en Límites Lógicos:** La autorización no sólo opera a nivel de endpoint (Spring Security), sino que se refuerza a nivel de dominio mediante un servicio verificador de pertenencia (`ValidacionService`), asegurando el aislamiento entre docentes, grupos y actividades.

```
┌────────────────────────────────────────────────────────────────────────┐
│                         CLIENTES EXTERNOS                              │
│    Navegador Web (Admin UI)  │  App Móvil/Desktop  │  Llamadas API     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ HTTP/REST (JSON, Multipart)
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         CAPA DE SEGURIDAD                              │
│   RateLimitFilter (Bucket4j)  ──►  JwtAuthenticationFilter (JJWT)     │
│   SecurityFilterChain: Stateless JWT (/api/**) vs Session (/admin/**)  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN / CONTROLADORES                 │
│  AuthController  │  AdminApi      │  ActividadApi  │  PreguntaApi       │
│  ProfesorApi     │  EstudianteApi │  PublicCursoApi│  RegistroApi       │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ DTOs / Payloads validados
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        CAPA DE LÓGICA DE NEGOCIO                       │
│  ValidacionService (Aislamiento)  │  ActividadService / EstudianteService│
│  AuthService                      │  RespuestaService / AsistenciaService │
│  Generadores de Reportes: ExcelReportGenerator (POI) / PdfReportGen    │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Entidades / Consultas Tipadas
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        CAPA DE PERSISTENCIA                            │
│  16 Spring Data JPA Repositories (Derived Queries + JPQL + Joins)     │
│  Hibernate ORM 6.x (Joined Table Inheritance, Transacciones ACID)      │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Conexión JDBC (Driver MySQL)
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           BASE DE DATOS                                │
│                     MySQL 8.0 (Motor InnoDB)                           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Organización por Capas y Responsabilidades

### 2.1 Capa de Seguridad y Filtros Perimetrales
- **Ubicación:** `com.rep.security.*`, `com.rep.config.SecurityConfig`.
- **Responsabilidad:**
  - Inspeccionar cada petición entrante antes de que alcance el despachador de Spring MVC (`DispatcherServlet`).
  - Aplicar restricción de tasa (*Rate Limiting*) mediante Bucket4j, protegiendo al sistema de abusos y ataques de fuerza bruta por IP.
  - Extraer y verificar la firma criptográfica HMAC-SHA de los tokens JWT de cabecera `Authorization: Bearer <token>`, resolviendo las autoridades y poblando el `SecurityContextHolder`.

### 2.2 Capa de Presentación (REST Controllers)
- **Ubicación:** `com.rep.controller.apis.*`.
- **Responsabilidad:**
  - Exponer las interfaces HTTP/REST según el estándar semántico (GET, POST, PUT, DELETE).
  - Recibir y deserializar cuerpos JSON y cargas multipart (archivos adjuntos).
  - Activar validación declarativa mediante `@Valid` y `jakarta.validation`.
  - Recoger la identidad del usuario autenticado mediante `@AuthenticationPrincipal Usuario usuario`.
  - Retornar instancias de `ResponseEntity<T>` con códigos de estado HTTP apropiados (`200 OK`, `201 CREATED`, `400 BAD REQUEST`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 NOT FOUND`, `500 INTERNAL SERVER ERROR`).

### 2.3 Capa de Servicio y Reglas de Negocio
- **Ubicación:** `com.rep.service.logica.*`, `com.rep.service.impl.*`, `com.rep.service.archivos.*`.
- **Responsabilidad:**
  - Ejecutar la lógica nuclear de la institución: orquestación de actividades, evaluación de respuestas de estudiantes, cálculo ponderado de notas, registro de asistencias y exportaciones de documentos.
  - Administrar límites transaccionales declarativos mediante `@Transactional`.
  - Aplicar validaciones de seguridad multi-dominio (`ValidacionService`), asegurando que ningún profesor pueda ver o alterar recursos ajenos a su asignación docente.

### 2.4 Capa de Persistencia y Acceso a Datos
- **Ubicación:** `com.rep.model.*`, `com.rep.repositories.*`.
- **Responsabilidad:**
  - Modelar el grafo de entidades de la base de datos y sus asociaciones relacionales (OneToMany, ManyToOne, UniqueConstraints).
  - Implementar el patrón Repository sobre Spring Data JPA, utilizando métodos derivados, especificaciones JPQL complejas con `JOIN FETCH` (para erradicar el problema N+1) y consultas nativas de inserción segura.
  - Gestionar auditoría temporal automática (`@CreatedDate`, `@EntityListeners(AuditingEntityListener.class)`).

### 2.5 Capa de Transformación (DTOs y Mappers)
- **Ubicación:** `com.rep.dto.*`, `com.rep.service.mapas.*`.
- **Responsabilidad:**
  - Prevenir la sobre-exposición de entidades JPA (*Over-posting* / *Information Leakage*).
  - Convertir colecciones y grafos circulares a estructuras planas JSON libres de recursión infinita.
  - Transformación automática de alto desempeño generada en tiempo de compilación con MapStruct (`CursoMapper`).

---

## 3. Patrones de Diseño Identificados en el Código

1. **Repository Pattern:** Todas las interacciones con MySQL están encapsuladas tras interfaces `JpaRepository<T, ID>`, aislando la lógica de negocio de los detalles de SQL.
2. **Joined Table Inheritance Pattern (Estrategia JPA JOINED):** La entidad base `Usuario` define los atributos comunes a todas las personas. Las entidades `Estudiante` y `Profesor` extienden a `Usuario` con tablas asociadas mediante claves foráneas compartidas (`@PrimaryKeyJoinColumn(name = "id")`).
3. **Token Bucket Pattern:** Implementado a través de `Bucket4j` en `RateLimitFilter` para aplicar ventanas de consumo y recarga de tokens por dirección IP cliente.
4. **Interceptor / Filter Chain Pattern:** El pipeline de seguridad de Spring Security y los filtros personalizados `OncePerRequestFilter` encadenan responsabilidades desacopladas.
5. **Data Transfer Object (DTO) Pattern:** Clases especializadas (`ActividadCreateDTO`, `RespuestaPreguntaDTO`, `ResultadoActividadDTO`) que protegen el contrato API de la estructura interna de la base de datos.
6. **Adapter / Mapper Pattern:** Utilizado mediante MapStruct (`CursoMapper`) para compilar adaptaciones eficientes entre `Curso` y `CursoDTO`.
7. **Controller Advice Pattern:** Centralización transversal de excepciones en `GlobalExceptionHandler` con generación de IDs únicos de seguimiento (*UUID trackingId*).
8. **Client Wrapper Pattern:** El proyecto incluye `ApiClientService` y `AdminApiServiceImpl` que actúan como adaptadores de llamadas HTTP usando `RestTemplate`, remanentes del soporte original a clientes de escritorio o clientes desacoplados.

---

## 4. Decisiones Arquitectónicas Comprobadas vs. Derivadas

### Decisiones Arquitectónicas Comprobadas (Verificadas en el Código)
- **Doble Esquema de Seguridad en Spring Security:** El sistema configura explícitamente dos cadenas de filtros con orden de precedencia:
  1. `apiSecurityFilterChain` (`@Order(1)`): Enfocada en las rutas de API REST (`/api/**`). Funciona de manera completamente **Stateless** (`SessionCreationPolicy.STATELESS`), validando tokens JWT e inyectando Rate Limiting.
  2. `adminWebSecurityFilterChain` (`@Order(2)`): Enfocada en el panel de administración web (`/admin/**`, `/api/admin/**`). Opera basada en **Sesión de Cookie JSESSIONID** (`SessionCreationPolicy.IF_REQUIRED`) y Form Login estándar (`/admin/login.html`).
- **Escala de Calificación Pedagógica:** La regla de negocio de evaluación fija la nota entre `0.0` y `5.0` (`@DecimalMin("0.0")`, `@DecimalMax("5.0")`), calculada de forma matemática sobre las preguntas objetivas y delegando las preguntas abiertas a revisión manual.
- **Relación Ternaria Fundamental:** La clase `ProfesorMateria` actúa como la entidad relacional nuclear que vincula a un docente con una asignatura y un grupo escolar determinado bajo una restricción de unicidad estricta en base de datos.
- **Almacenamiento Local de Adjuntos:** Los archivos de preguntas y opciones se persisten en el sistema de archivos del servidor bajo el directorio del usuario `$HOME/.rep/uploads/`.

### Interpretaciones y Hechos de Evolución Histórica
- **Transición de Desktop JavaFX a Web SPA:** El código mantiene dependencias y servicios clientes (`ApiClientService`, `PreguntaApiClient`, `AuthClientServiceImpl`) diseñados originalmente para consumir una API remota desde un cliente JavaFX de escritorio. Tras migrar a una interfaz web estática en Spring Boot, estos clientes quedaron inactivos para el runtime web pero permanecen compilables en el paquete `service/funciones/`.
- **Doble Motor de Generación PDF:** Se observa la presencia concurrente de `iText 7.2.5` (`PdfReportGenerator`) y `OpenPDF / iText 2.1.7` (`RespuestaServiceImpl.generatePromediosPdfReport`), lo cual denota una migración gradual de librerías no completada.
