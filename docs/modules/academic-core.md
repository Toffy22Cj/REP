# 🏫 Módulo Académico Central (Academic Core)

## 1. Propósito y Alcance

El módulo **Academic Core** encapsula la estructura organizativa y curricular de la institución educativa. Modela la jerarquía escolar compuesta por grados y grupos (Cursos), el catálogo de asignaturas (Materias), y el mecanismo formal de asignación lectiva que confiere autoridad pedagógica a los docentes sobre grupos concretos (`ProfesorMateria`).

---

## 2. Entidades del Dominio

```
┌─────────────────┐           ┌──────────────────────┐           ┌──────────────────┐
│     Curso       │           │   ProfesorMateria    │           │     Materia      │
├─────────────────┤           ├──────────────────────┤           ├──────────────────┤
│ id: Long (PK)   │1         *│ id: Long (PK)        │*         1│ id: Long (PK)    │
│ grado: Integer  │◄──────────│ curso_id (FK)        │──────────►│ nombre: String UK│
│ grupo: String   │           │ profesor_id (FK)     │           └──────────────────┘
└────────┬────────┘           │ materia_id (FK)      │
         │1                   └──────────────────────┘
         │                               ▲
         │*                              │*
┌────────▼────────┐                      │1
│   Estudiante    │             ┌────────┴─────────┐
├─────────────────┤             │     Profesor     │
│ curso_id (FK)   │             └──────────────────┘
└─────────────────┘
```

### 2.1 Entidad `Curso` ([Curso.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/model/Curso.java))
- **Tabla:** `cursos`.
- **Atributos:**
  - `id` (`Long`, Clave Primaria Autoincremental).
  - `grado` (`Integer`, Obligatorio, `@Min(1)`, `@Max(12)`): Representa el año académico (ej. 1° a 11° o 12°).
  - `grupo` (`String`, Obligatorio, `@Size(max = 5)`): Identificador del paralelo o sección (ej. "A", "B", "1", "02").
  - `estudiantes` (`List<Estudiante>`, `@OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)`).
- **Métodos Notables:**
  - `getNombreCompleto()` / `getNombre()`: Retorna la representación legible combinada `grado + "-" + grupo` (ej. "10-A").
  - `addEstudiante(Estudiante estudiante)`: Método de conveniencia para mantener sincronizada la relación bidireccional.

### 2.2 Entidad `Materia` ([Materia.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/model/Materia.java))
- **Tabla:** `materias`.
- **Atributos:**
  - `id` (`Long`, Clave Primaria Autoincremental).
  - `nombre` (`String`, Obligatorio, Único en BD, longitud entre 3 y 100 caracteres).

### 2.3 Entidad `ProfesorMateria` ([ProfesorMateria.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/model/ProfesorMateria.java))
- **Tabla:** `profesor_materia`.
- **Restricción de Unicidad:** `@UniqueConstraint(columnNames = {"profesor_id", "materia_id", "curso_id"})`.
- **Atributos:**
  - `id` (`Long`, Clave Primaria Autoincremental).
  - `profesor` (`Profesor`, `@ManyToOne`, Clave Foránea `fk_profesor`, obligatorio).
  - `materia` (`Materia`, `@ManyToOne`, Clave Foránea `fk_materia`, obligatorio).
  - `curso` (`Curso`, `@ManyToOne`, Clave Foránea `fk_curso`, obligatorio).
- **Sobrescritura de `equals` y `hashCode`:** Basada exclusivamente en la igualdad de la tríada `(profesor, materia, curso)` para prevenir duplicidades lógicas en colecciones.

---

## 3. Servicios y Reglas de Negocio

### 3.1 `CursoServiceImpl` ([CursoServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/CursoServiceImpl.java))
- **Regla de Unicidad:** Antes de guardar o actualizar un curso, invoca `cursoRepository.existsByGradoAndGrupo(...)`. Si existe un registro con id distinto, lanza `IllegalStateException("Ya existe un curso con este grado y grupo")`.
- **Integridad Referencial en Eliminación:** Antes de borrar un curso (`eliminarCurso`), verifica si existen asignaciones docentes activas con `profesorMateriaRepository.existsByCursoId(id)`. Si es positivo, aborta arrojando `IllegalStateException("No se puede eliminar el curso porque tiene materias asignadas")`.
- **Métricas:** `contarEstudiantesPorCurso()` ejecuta la consulta agregada `cursoRepository.contarEstudiantesPorCurso()` que proyecta el total de estudiantes inscritos por curso.

### 3.2 `CursoMapper` ([CursoMapper.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/mapas/CursoMapper.java))
Utiliza el procesador de anotaciones de MapStruct para generar código Java libre de reflexión:
```java
@Mapper(componentModel = "spring")
public interface CursoMapper {
    @Mapping(target = "cantidadEstudiantes", 
             expression = "java(curso.getEstudiantes() != null ? (long) curso.getEstudiantes().size() : 0L)")
    CursoDTO toDto(Curso curso);

    @InheritInverseConfiguration
    @Mapping(target = "estudiantes", ignore = true)
    Curso toEntity(CursoDTO cursoDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estudiantes", ignore = true)
    void updateCursoFromDto(CursoDTO cursoDTO, @MappingTarget Curso curso);
}
```

### 3.3 `MateriaServiceImpl` ([MateriaServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/MateriaServiceImpl.java))
- `getMateriasByProfesor(Long profesorId)`: Resuelve mediante consulta relacional inversa (`materiaRepository.findByProfesorId(profesorId)`) el listado de materias que el docente imparte en al menos un curso.

---

## 4. Auditoría y Trazabilidad de Cambios

Todas las operaciones mutativas ejecutadas sobre el núcleo académico desde [AdminApi.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/controller/apis/AdminApi.java) persisten un registro inmutable en la tabla `audit_logs` mediante el método `registrarAuditoria(String action, String details)`:
- Creación de curso: `registrarAuditoria("CREAR_CURSO", "ID: 3, 10-A")`.
- Actualización de curso: `registrarAuditoria("ACTUALIZAR_CURSO", "ID: 3, 10-B")`.
- Eliminación de curso: `registrarAuditoria("ELIMINAR_CURSO", "ID: 3")`.
- Creación de materia: `registrarAuditoria("CREAR_MATERIA", "ID: 5, Filosofía")`.
- Eliminación de materia: `registrarAuditoria("ELIMINAR_MATERIA", "ID: 5, Filosofía")`.
- Asignación de docente: `registrarAuditoria("ASIGNAR_DOCENTE", "Profesor: 10, Materia: 5, Curso: 3")`.
- Revocación de asignación: `registrarAuditoria("ELIMINAR_ASIGNACION", "ID: 12")`.
