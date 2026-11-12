# 📚 Sistema REP - Documentación Técnica y Arquitectónica de Referencia

> **Documento Maestro de Arquitectura, Ingeniería de Software, Persistencia, API y Operaciones**  
> **Versión del Sistema:** 1.0 (Producción)  
> **Línea Base del Código:** Spring Boot 3.2.0 | Java 17 LTS | MySQL 8.0  
> **Fecha de Análisis:** Noviembre 2026  
> **Clasificación:** Documentación Técnica de Nivel Arquitectura y Producción

---

## 1. Introducción y Propósito del Sistema

El sistema **REP** (Registro Educativo Pedagógico) es una plataforma integral de gestión académica y evaluación pedagógica escolar construida sobre el ecosistema **Spring Boot 3.2.0** y **Java 17 LTS**. Su propósito primordial es orquestar de manera centralizada la administración institucional, la trazabilidad de asignaturas y cursos, el control de acceso basado en roles (RBAC), el seguimiento de asistencia diaria, y un motor interactivo de creación, asignación y calificación automática/manual de actividades evaluativas con soporte para archivos multimedia y pictogramas (ARASAAC).

### Problema que Resuelve
1. **Dispersión Operativa:** Unifica la administración de cursos, grupos, materias y cuerpos docentes en un modelo relacional normalizado con auditoría automática.
2. **Ciclo Evaluativo Desarticulado:** Proporciona un flujo continuo para diseñar exámenes, quices y talleres (opción múltiple, verdadero/falso, respuesta abierta), recopilar entregas de estudiantes y calcular notas preliminares automáticas en escala decimal 0.0 - 5.0.
3. **Control de Acceso y Multitenencia Lógica:** Garantiza que los profesores sólo interactúen con estudiantes, actividades y calificaciones vinculadas a los cursos y asignaturas formalmente asignados a su carga lectiva.
4. **Respaldo y Reportabilidad:** Permite generar reportes ejecutivos e históricos en formatos PDF (iText/OpenPDF), hojas de cálculo Excel (Apache POI) y archivos delimitados por comas (CSV).

---

## 2. Mapa y Estructura de la Documentación

Toda la documentación técnica se encuentra modularizada y clasificada para permitir una inmersión inmediata de arquitectos, desarrolladores backend/frontend, analistas de seguridad y operadores DevOps:

```
docs/
├── README.md                           # Índice maestro, mapa de navegación y glosario
├── architecture/
│   ├── overview.md                     # Visión arquitectónica, capas, decisiones y patrones
│   ├── components.md                   # Catálogo exhaustivo de componentes y subsistemas
│   └── diagrams.md                     # Diagramas C4, UML de clases y ERD en Mermaid
├── modules/
│   ├── auth-security.md                # Autenticación, JWT, UserDetails y Rate Limiting
│   ├── academic-core.md                # Cursos, Materias, Asignaciones Profesor-Materia
│   ├── activities-evaluation.md        # Actividades, Preguntas, Opciones y Motor de Corrección
│   ├── student-management.md           # Ciclo del Estudiante, Estados y Transferencias
│   ├── attendance-reports.md           # Asistencias y Motores de Reportes PDF/Excel/CSV
│   └── admin-system.md                 # Panel Web Administrativo, Auditoría y Control del Sistema
├── flows/
│   └── business-flows.md               # 7 Flujos de ejecución reconstruidos paso a paso
├── database/
│   └── data-model.md                   # Diccionario de datos, tablas, relaciones y restricciones
├── api/
│   ├── endpoints.md                    # Catálogo formal de endpoints HTTP REST
│   └── client-sdk-feign.md             # Capa de clientes internos (RestTemplate/Feign SDK)
├── security/
│   └── security.md                     # Auditoría de seguridad, filtros, RBAC y vulnerabilidades
├── deployment/
│   └── installation.md                 # Guía de instalación, variables de entorno y despliegue
├── testing/
│   └── testing.md                      # Estrategia de pruebas, brecha de cobertura y suite de test
└── maintenance/
    ├── error-handling.md               # Manejo global de excepciones y códigos de rastreo
    ├── extensibility.md                # Guía para extender entidades, servicios y endpoints
    └── technical-debt.md               # Auditoría técnica, limitaciones y plan de remediación
```

---

## 3. Resumen Ejecutivo del Stack Tecnológico

| Componente | Tecnología | Versión | Propósito en el Sistema |
| :--- | :--- | :--- | :--- |
| **Lenguaje Base** | Java OpenJDK | 17 LTS | Plataforma de cómputo del backend |
| **Framework Backend** | Spring Boot | 3.2.0 | Framework corporativo base |
| **Seguridad** | Spring Security | 6.2.0 | Control de acceso, sesiones y cadenas de filtros |
| **Autenticación** | JJWT (io.jsonwebtoken) | 0.11.5 | Generación y parseo de JSON Web Tokens (HMAC-SHA) |
| **Persistencia** | Spring Data JPA / Hibernate | 6.x | ORM, mapeo entidad-relacional y repositorios |
| **Motor de BD** | MySQL Server | 8.0 | Base de datos relacional InnoDB transaccional |
| **Rate Limiting** | Bucket4j Core | 8.1.1 | Algoritmo Token Bucket para prevención de ataques DoS/Brute Force |
| **Mapeo de Objetos** | MapStruct | 1.5.5.Final | Transformación tipada compile-time DTO <-> Entidad |
| **Boilerplate** | Project Lombok | 1.18.30 | Reducción de código Java (@Data, @Slf4j, etc.) |
| **Reportes PDF 1** | iText Core | 7.2.5 | Generación moderna de documentos PDF de resultados |
| **Reportes PDF 2** | OpenPDF / iText Legacy | 2.1.7 | Generación histórica de reportes de promedios |
| **Reportes Excel** | Apache POI OOXML | 5.2.3 | Generación de libros de trabajo en formato .xlsx |
| **Documentación API**| Springdoc OpenAPI | 2.5.0 | Especificación OpenAPI 3.0 y UI Swagger interactiva |
| **Frontend Admin** | Vanilla JavaScript / CSS3 | ES6+ Modules | Interfaz web responsiva sin dependencias npm |

---

## 4. Convenciones de Organización del Repositorio

- **`src/main/java/com/rep/`**: Código fuente backend estructurado bajo una arquitectura de capas bien delimitada:
  - `config/`: Beans de configuración (CORS, Security, JWT, BrowserLauncher, RestTemplate, CommandLineRunner).
  - `controller/apis/`: Controladores REST organizados por dominios funcionales (Admin, Profesor, Estudiante, Actividades, Preguntas, Auth, Registro, Público, Sistema).
  - `dto/`: Objetos de transferencia de datos clasificados por submódulos.
  - `exception/`: Jerarquía de excepciones de dominio y asesor `@ControllerAdvice` global.
  - `model/`: Entidades JPA de base de datos con herencia `@Inheritance(strategy = InheritanceType.JOINED)`.
  - `repositories/`: Interfaces Spring Data JPA con consultas derivadas y JPQL optimizadas.
  - `security/`: Filtros HTTP de bajo nivel (`JwtAuthenticationFilter`, `RateLimitFilter`).
  - `service/`: Lógica de negocio dividida en interfaces (`logica/`), implementaciones (`impl/`), generadores de archivos (`archivos/`), clientes API (`funciones/`) y mapeadores (`mapas/`).
- **`src/main/resources/`**:
  - `application.properties`: Configuración de base de datos, JPA, JWT, logging y multipart.
  - `application-dev.properties`: Perfil local para desarrolladores.
  - `static/admin/`: Aplicación de interfaz administrativa en HTML5, CSS3 y JavaScript moderno modular.
- **`src/test/java/com/rep/`**: Pruebas de integración de contexto Spring.

---

## 5. Glosario Técnico Institucional

- **Actividad:** Entidad evaluativa (`EXAMEN`, `QUIZ`, `TALLER`) que contiene un conjunto de preguntas y pertenece a una carga académica docente.
- **ARASAAC:** Sistema pictográfico de comunicación aumentativa y alternativa cuyos recursos gráficos se integran en las preguntas y opciones para accesibilidad.
- **Asignación (ProfesorMateria):** Tupla ternaria obligatoria `(profesor_id, materia_id, curso_id)` que define legalmente qué docente tiene jurisdicción académica sobre qué grupo y asignatura.
- **Bucket4j:** Biblioteca Java que implementa el algoritmo de "Token Bucket" para dosificar y limitar la frecuencia de peticiones HTTP por IP.
- **JOINED Inheritance:** Estrategia JPA donde la tabla base `usuarios` almacena atributos comunes y las tablas secundarias `estudiantes` y `profesores` contienen únicamente atributos derivados compartiendo la clave primaria.
- **JWT (JSON Web Token):** Token criptográfico firmado con clave secreta HMAC-SHA utilizado como credencial Bearer sin estado en la API REST.
- **OpenPDF vs iText 7:** Coexistencia de dos motores de renderizado PDF independientes dentro del mismo classpath del proyecto.
- **ProfesorMateria:** Componente pivote del modelo de datos; actúa como ancla de seguridad y autorización en todo el sistema.
- **Rate Limit:** Mecanismo de defensa contra fuerza bruta: 5 intentos cada 15 min en `/api/auth/login`; 100 requests por minuto en rutas generales `/api/*`.
- **RBAC (Role-Based Access Control):** Esquema de autorización donde los usuarios poseen exactamente uno de tres roles: `ADMIN`, `PROFESOR` o `ESTUDIANTE`.
