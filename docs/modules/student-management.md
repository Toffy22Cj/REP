# 🎓 Módulo de Gestión de Estudiantes y Trazabilidad

## 1. Propósito y Alcance

El módulo de **Gestión de Estudiantes** administra la información personal, estado institucional, afiliación a cursos escolares, historial de calificaciones y notificaciones de los alumnos matriculados en la institución.

---

## 2. Modelo de Herencia y Persistencia JPA

En la base de datos MySQL, el estudiante no es una entidad aislada, sino una especialización de la entidad `Usuario` bajo una estrategia relacional **JOINED** (`@Inheritance(strategy = InheritanceType.JOINED)`):

```
       ┌───────────────────────────────┐
       │           usuarios            │
       ├───────────────────────────────┤
       │ id (PK autoincrement)         │
       │ identificacion, correo, clave │
       │ nombre, apellido, rol, activo │
       └───────────────▲───────────────┘
                       │ Clave Foránea compartida (id)
       ┌───────────────┴───────────────┐
       │          estudiantes          │
       ├───────────────────────────────┤
       │ id (PK, FK -> usuarios.id)    │
       │ curso_id (FK -> cursos.id)    │
       │ estado: ENUM                  │
       │ edad: Integer                 │
       └───────────────────────────────┘
```

### Sincronización de Edad entre Tablas
Debido a que el campo `edad` existía históricamente tanto en la tabla general `usuarios` como en `estudiantes`, [Estudiante.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/model/Estudiante.java) implementa hooks de ciclo de vida JPA para garantizar su paridad:
```java
@PostLoad
private void postLoad() {
    if (this.edadEstudiante != null) {
        super.setEdad(this.edadEstudiante);
    }
}

@PrePersist
@PreUpdate
protected void prePersistEstudiante() {
    if (super.getEdad() != null) {
        this.edadEstudiante = super.getEdad();
    }
}
```

---

## 3. Estados del Estudiante (`EstadoEstudiante`)

El estudiante transita por un enum de estados rigurosamente tipado:
- `activo`: Estudiante matriculado que puede iniciar sesión, consultar materias y resolver actividades evaluativas.
- `retirado`: Estudiante desvinculado; sus registros históricos de respuestas y asistencias se preservan intactos para auditoría.
- `graduado`: Estudiante que ha completado satisfactoriamente el ciclo académico.
- `suspendido`: Estudiante con restricción disciplinaria o administrativa temporal.

---

## 4. Transferencias de Curso entre Grados y Grupos

Desde [AdminApi.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/controller/apis/AdminApi.java) (`PUT /api/admin/estudiantes/{id}/curso`):
1. Se verifica la existencia del estudiante.
2. Se verifica la existencia del curso destino (`nuevoCursoId`).
3. Se actualiza la referencia foránea `estudiante.setCurso(nuevoCurso)`.
4. Se registra el cambio en la auditoría del sistema:
   `registrarAuditoria("CAMBIO_CURSO_ESTUDIANTE", "Estudiante ID: " + id + ", Nuevo Curso ID: " + nuevoCursoId)`.

---

## 5. Exportación de Listas de Curso a CSV

En [EstudianteServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/EstudianteServiceImpl.java):
El método `exportEstudiantesByCursoToCsv(Long cursoId)` genera dinámicamente un archivo de texto plano delimitado por comas con formato RFC 4180:
```csv
ID,Nombre,Correo,Estado
15,"Juan Pérez",juan.perez@colegio.edu.co,activo
18,"María Rodríguez",maria.rodriguez@colegio.edu.co,activo
```
Este endpoint (`GET /api/profesor/estudiantes/curso/{cursoId}/export`) inyecta la cabecera HTTP:
`Content-Disposition: attachment; filename=estudiantes_curso_{cursoId}.csv`.

---

## 6. Subsistema de Notificaciones

La entidad `Notificacion` registra alertas académicas para los alumnos:
- **Tipos de Notificación:**
  - `NUEVA_ACTIVIDAD`: Disparada cuando un docente publica una tarea o examen.
  - `CALIFICACION`: Disparada cuando una actividad es revisada y calificada.
  - `MENSAJE`: Mensaje institucional o docente directo.
  - `RECORDATORIO`: Alerta de proximidad de fecha límite.
- **Trazabilidad de Lectura:** Bandera booleana `leida`, actualizable mediante `notificacionRepository.marcarComoLeida(notificacionId, estudianteId)`.
