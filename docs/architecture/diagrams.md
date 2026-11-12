# 📊 Diagramas Arquitectónicos del Sistema REP

Este documento reúne las representaciones visuales del sistema utilizando diagramas formales en estándar Mermaid, reflejando fielmente la estructura del código y el modelo de datos real.

---

## 1. Diagrama de Contexto del Sistema (C4 - Nivel 1)

```mermaid
flowchart TD
    subgraph Actores["Actores del Sistema"]
        Admin["Administrador / Rector"]
        Docente["Profesor"]
        Alumno["Estudiante"]
    end

    subgraph PlataformaREP["Sistema REP (Registro Educativo Pedagógico)"]
        REP["Servidor Monolítico Spring Boot 3.2.0<br/>API REST + Panel de Control Web"]
    end

    subgraph Almacenamiento["Persistencia y Archivos"]
        MySQL[("Base de Datos Relacional<br/>MySQL 8.0 (InnoDB)")]
        DiskFS["Almacenamiento Local en Disco<br/>Uploads y Pictogramas ($HOME/.rep/uploads)"]
    end

    Admin -->|"Gestiona Cursos, Materias, Usuarios y Asignaciones"| REP
    Docente -->|"Crea Actividades, Sube Adjuntos, Califica y Pasa Lista"| REP
    Alumno -->|"Consulta Asignaturas, Resuelve Actividades y Revisa Notas"| REP

    REP -->|"Persistencia JPA / Hibernate"| MySQL
    REP -->|"Guarda y Lee Archivos Adjuntos / Imágenes"| DiskFS
```

---

## 2. Diagrama de Contenedores (C4 - Nivel 2)

```mermaid
flowchart TD
    subgraph Cliente["Navegador Web / Clientes"]
        WebAdmin["Panel Web de Administración<br/>(HTML5 / CSS3 / Vanilla JS ES6)"]
        ClienteAPI["Clientes REST Externos<br/>(Móvil / Desktop / Scripts)"]
    end

    subgraph Servidor["Aplicación Spring Boot (JVM 17)"]
        FiltrosSeguridad["Filtros Perimetrales<br/>RateLimitFilter & JwtAuthenticationFilter"]
        ControladoresREST["Capa de Exposición REST<br/>9 Controladores REST"]
        ServiciosNegocio["Capa de Lógica de Negocio<br/>Servicios Transaccionales y Validadores"]
        GeneradoresReportes["Motores de Documentos<br/>iText 7 / OpenPDF / Apache POI"]
        AccesoDatos["Capa de Datos<br/>Spring Data JPA Repositories"]
    end

    subgraph Infraestructura["Infraestructura y Datos"]
        BD[("MySQL 8.0<br/>Esquema: colegio")]
        FS["Filesystem Local<br/>Directorio: ~/.rep/uploads"]
    end

    WebAdmin -->|"HTTP Form / Cookies Session (/admin/**)"| FiltrosSeguridad
    ClienteAPI -->|"HTTPS / Bearer JWT (/api/**)"| FiltrosSeguridad

    FiltrosSeguridad --> ControladoresREST
    ControladoresREST --> ServiciosNegocio
    ServiciosNegocio --> GeneradoresReportes
    ServiciosNegocio --> AccesoDatos
    AccesoDatos -->|"JDBC / TCP 3306"| BD
    ServiciosNegocio -->|"I/O Streams"| FS
```

---

## 3. Diagrama de Componentes del Backend (C4 - Nivel 3)

```mermaid
flowchart LR
    subgraph SecurityLayer["Seguridad y Filtros"]
        RLF["RateLimitFilter"]
        JAF["JwtAuthenticationFilter"]
        SC["SecurityConfig"]
        CUD["CustomUserDetailsService"]
    end

    subgraph Controllers["Controladores API"]
        AC["AuthController"]
        AA["AdminApi"]
        ActA["ActividadApi"]
        PA["PreguntaApi"]
        ProfA["ProfesorApi"]
        EA["EstudianteApi"]
        SA["SystemApi"]
    end

    subgraph Services["Servicios de Negocio"]
        AuthS["AuthService"]
        ValS["ValidacionService"]
        ActS["ActividadServiceImpl"]
        PregS["PreguntaServiceImpl"]
        EstS["EstudianteServiceImpl"]
        RespS["RespuestaServiceImpl"]
        AsisS["AsistenciaServiceImpl"]
        CorrS["CorreccionService"]
        RepG["Excel & Pdf Report Generators"]
    end

    subgraph Repos["Repositorios Spring Data"]
        URepo[("UsuarioRepository")]
        CRepo[("CursoRepository")]
        MRepo[("MateriaRepository")]
        PMRepo[("ProfesorMateriaRepository")]
        ActRepo[("ActividadRepository")]
        PregRepo[("PreguntaRepository")]
        OpRepo[("OpcionRepository")]
        RespEstRepo[("RespuestaEstudianteRepository")]
        RespPregRepo[("RespuestaPreguntaRepository")]
        AsisRepo[("AsistenciaRepository")]
        AuditRepo[("AuditLogRepository")]
    end

    RLF --> JAF
    JAF --> Controllers

    AC --> AuthS
    AuthS --> URepo

    AA --> URepo & CRepo & MRepo & PMRepo & AuditRepo

    ActA --> ActS & ValS & EstS
    PA --> PregS & ValS
    ProfA --> RespS & AsisS & ValS & RepG
    EA --> EstS

    ActS --> ActRepo & PMRepo & RespEstRepo
    PregS --> PregRepo & OpRepo & RespPregRepo
    EstS --> EstRepo & ActRepo & RespEstRepo & CorrS
    RespS --> RespEstRepo & RespPregRepo & RepG
    AsisS --> AsisRepo & PMRepo
```

---

## 4. Diagrama de Clases del Dominio Principal (UML)

```mermaid
classDiagram
    class Usuario {
        +Long id
        +String nombre
        +String apellido
        +String correo
        +String identificacion
        +TipoIdentificacion tipoIdentificacion
        +String contraseña
        +Rol rol
        +Integer edad
        +Boolean activo
        +LocalDateTime creadoEn
        +isActivo() boolean
        +getAuthorities() Collection
    }

    class Estudiante {
        +Integer edadEstudiante
        +EstadoEstudiante estado
        +getCursoNombre() String
    }

    class Profesor {
        +Integer edadProfesor
        +LocalDate fechaIngreso
        +EstadoProfesor estado
    }

    class Curso {
        +Long id
        +Integer grado
        +String grupo
        +getNombreCompleto() String
        +addEstudiante(Estudiante) void
    }

    class Materia {
        +Long id
        +String nombre
    }

    class ProfesorMateria {
        +Long id
    }

    class Actividad {
        +Long id
        +String titulo
        +TipoActividad tipo
        +String descripcion
        +LocalDate fechaEntrega
        +LocalTime horaEntrega
        +Integer duracionMinutos
        +Boolean permitirReintentos
        +Boolean activa
        +LocalDateTime fechaCreacion
        +getFechaHoraEntrega() LocalDateTime
    }

    class Pregunta {
        +Long id
        +String enunciado
        +TipoPregunta tipo
        +String archivoUrl
        +String archivoTipo
        +addOpcion(Opcion) void
    }

    class Opcion {
        +Long id
        +String texto
        +Boolean esCorrecta
        +String archivoUrl
        +String archivoTipo
        +Boolean tieneArchivo
        +marcarComoCorrecta() void
    }

    class RespuestaEstudiante {
        +Long id
        +Float nota
        +Boolean entregado
        +LocalDateTime fechaInicio
        +LocalDateTime fechaEntrega
        +Integer duracionMinutos
        +String observaciones
        +addRespuestaPregunta(RespuestaPregunta) void
    }

    class RespuestaPregunta {
        +Long id
        +String respuestaAbierta
        +String archivoAdjunto
        +String nombreArchivo
        +Boolean esCorrecta
    }

    class Asistencia {
        +Long id
        +LocalDate fecha
        +EstadoAsistencia estado
        +String tipoExcusa
        +String observacion
    }

    class AuditLog {
        +Long id
        +String username
        +String action
        +String details
        +LocalDateTime timestamp
    }

    Usuario <|-- Estudiante : JOINED
    Usuario <|-- Profesor : JOINED

    Curso "1" <-- "*" Estudiante : curso
    Profesor "1" <-- "*" ProfesorMateria : profesor
    Materia "1" <-- "*" ProfesorMateria : materia
    Curso "1" <-- "*" ProfesorMateria : curso

    ProfesorMateria "1" <-- "*" Actividad : profesorMateria
    Actividad "1" *-- "*" Pregunta : preguntas
    Pregunta "1" *-- "*" Opcion : opciones

    Estudiante "1" <-- "*" RespuestaEstudiante : estudiante
    Actividad "1" <-- "*" RespuestaEstudiante : actividad
    Profesor "0..1" <-- "*" RespuestaEstudiante : calificadoPor
    RespuestaEstudiante "1" *-- "*" RespuestaPregunta : respuestasPreguntas

    Pregunta "1" <-- "*" RespuestaPregunta : pregunta
    Opcion "0..1" <-- "*" RespuestaPregunta : opcion
    Estudiante "1" <-- "*" RespuestaPregunta : estudiante

    ProfesorMateria "1" <-- "*" Asistencia : profesorMateria
    Estudiante "1" <-- "*" Asistencia : estudiante
```

---

## 5. Diagrama Entidad-Relación de Base de Datos (ERD)

```mermaid
erDiagram
    usuarios ||--o| estudiantes : "id PK/FK"
    usuarios ||--o| profesores : "id PK/FK"
    cursos ||--o{ estudiantes : "curso_id"

    profesores ||--o{ profesor_materia : "profesor_id"
    materias ||--o{ profesor_materia : "materia_id"
    cursos ||--o{ profesor_materia : "curso_id"

    profesor_materia ||--o{ actividades : "profesor_materia_id"
    actividades ||--o{ preguntas : "actividad_id"
    preguntas ||--o{ opciones : "pregunta_id"

    estudiantes ||--o{ respuestas_estudiante : "estudiante_id"
    actividades ||--o{ respuestas_estudiante : "actividad_id"
    profesores ||--o{ respuestas_estudiante : "calificado_por"

    respuestas_estudiante ||--o{ respuestas_pregunta : "respuesta_estudiante_id"
    preguntas ||--o{ respuestas_pregunta : "pregunta_id"
    opciones ||--o{ respuestas_pregunta : "opcion_id"
    estudiantes ||--o{ respuestas_pregunta : "estudiante_id"

    profesor_materia ||--o{ asistencias : "profesor_materia_id"
    estudiantes ||--o{ asistencias : "estudiante_id"

    usuarios {
        bigint id PK
        varchar nombre
        varchar apellido
        varchar correo UK
        varchar identificacion UK
        varchar tipo_identificacion
        varchar contraseña
        varchar rol
        int edad
        datetime creado_en
        date fecha_ingreso
        boolean activo
        varchar sexo
    }

    estudiantes {
        bigint id PK,FK
        bigint curso_id FK
        enum estado
        int edad
    }

    profesores {
        bigint id PK,FK
        int edad
        date fecha_ingreso
        varchar estado
    }

    cursos {
        bigint id PK
        int grado
        varchar grupo
    }

    materias {
        bigint id PK
        varchar nombre UK
    }

    profesor_materia {
        bigint id PK
        bigint profesor_id FK
        bigint materia_id FK
        bigint curso_id FK
    }

    actividades {
        bigint id PK
        bigint profesor_materia_id FK
        varchar titulo
        enum tipo
        varchar descripcion
        date fecha_entrega
        time hora_entrega
        int duracion_minutos
        boolean permitir_reintentos
        boolean activa
        datetime fecha_creacion
    }

    preguntas {
        bigint id PK
        bigint actividad_id FK
        varchar enunciado
        varchar tipo
        varchar archivo_url
        varchar archivo_tipo
    }

    opciones {
        bigint id PK
        bigint pregunta_id FK
        varchar texto
        boolean es_correcta
        varchar archivo_url
        varchar archivo_tipo
        varchar nombre_archivo
        boolean tiene_archivo
    }

    respuestas_estudiante {
        bigint id PK
        bigint estudiante_id FK
        bigint actividad_id FK
        bigint calificado_por FK
        float nota
        boolean entregado
        datetime fecha_inicio
        datetime fecha_entrega
        int duracion_minutos
        varchar observaciones
    }

    respuestas_pregunta {
        bigint id PK
        bigint respuesta_estudiante_id FK
        bigint pregunta_id FK
        bigint estudiante_id FK
        bigint opcion_id FK
        varchar respuesta_abierta
        varchar archivo_adjunto
        varchar nombre_archivo
        boolean es_correcta
    }

    asistencias {
        bigint id PK
        bigint profesor_materia_id FK
        bigint estudiante_id FK
        date fecha
        varchar estado
        varchar tipo_excusa
        text observacion
    }

    audit_logs {
        bigint id PK
        varchar username
        varchar action
        text details
        datetime timestamp
    }
```

---

## 6. Diagrama de Estado: Ciclo de Vida de una Actividad y Entrega

```mermaid
stateDiagram-v2
    [*] --> Creada : Docente crea actividad (Activa = true)
    
    Creada --> ConReactivos : Docente agrega preguntas y opciones
    ConReactivos --> Publicada : Fecha/hora actual >= fechaCreacion
    
    Publicada --> EnResolucion : Estudiante inicia resolución
    
    EnResolucion --> EvaluadaAutomaticamente : Estudiante envía respuestas objetivas
    EnResolucion --> PendienteRevisionDocente : Estudiante envía respuestas abiertas
    
    EvaluadaAutomaticamente --> Cerrada : Nota preliminar calculada (0.0 - 5.0)
    PendienteRevisionDocente --> Cerrada : Docente asigna nota final manual
    
    Publicada --> Expirada : Fecha/hora actual > fechaHoraEntrega
    Cerrada --> [*]
    Expirada --> [*]
```
