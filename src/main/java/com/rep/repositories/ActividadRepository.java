package com.rep.repositories;

import com.rep.model.Actividad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    long countByProfesorMateriaProfesorId(Long profesorId);
    long countByProfesorMateriaProfesorIdAndFechaCreacionAfter(Long profesorId, LocalDateTime fecha);

    @Query("SELECT a FROM Actividad a " +
            "WHERE LOWER(a.titulo) LIKE LOWER(concat('%', :titulo, '%')) " +
            "AND a.profesorMateria.profesor.id = :profesorId")
    List<Actividad> findByTituloAndProfesorId(
            @Param("titulo") String titulo,
            @Param("profesorId") Long profesorId);

    // Consultas corregidas (usando profesorMateria.profesor.id en lugar de profesor.id)
    @Query("SELECT a FROM Actividad a WHERE " +
            "a.profesorMateria.materia.id = :materiaId AND " +
            "a.profesorMateria.curso.id = :cursoId AND " +
            "a.profesorMateria.profesor.id = :profesorId")
    List<Actividad> findByProfesorMateriaMateriaIdAndProfesorMateriaCursoIdAndProfesorId(
            @Param("materiaId") Long materiaId,
            @Param("cursoId") Long cursoId,
            @Param("profesorId") Long profesorId);

    @Query("SELECT a FROM Actividad a WHERE " +
            "a.profesorMateria.materia.id = :materiaId AND " +
            "a.profesorMateria.profesor.id = :profesorId")
    List<Actividad> findByProfesorMateriaMateriaIdAndProfesorId(
            @Param("materiaId") Long materiaId,
            @Param("profesorId") Long profesorId);

    @Query("SELECT a FROM Actividad a WHERE " +
            "a.profesorMateria.curso.id = :cursoId AND " +
            "a.profesorMateria.profesor.id = :profesorId")
    List<Actividad> findByProfesorMateriaCursoIdAndProfesorId(
            @Param("cursoId") Long cursoId,
            @Param("profesorId") Long profesorId);

    @Query("SELECT a FROM Actividad a WHERE " +
            "a.profesorMateria.profesor.id = :profesorId")
    List<Actividad> findByProfesorId(@Param("profesorId") Long profesorId);
    boolean existsByIdAndProfesorMateria_Profesor_Id(Long actividadId, Long profesorId);

    @Query("SELECT a.profesorMateria.profesor.id FROM Actividad a WHERE a.id = :actividadId")
    Optional<Long> findProfesorIdByActividadId(@Param("actividadId") Long actividadId);
    // Métodos existentes que no necesitan cambios
    List<Actividad> findByProfesorMateriaMateriaId(Long materiaId);
    List<Actividad> findByProfesorMateriaCursoId(Long cursoId);
    List<Actividad> findByProfesorMateriaMateriaIdAndProfesorMateriaCursoId(Long materiaId, Long cursoId);
    @Query("SELECT a FROM Actividad a WHERE a.profesorMateria.curso.id = :cursoId AND a.profesorMateria.materia.id = :materiaId")
    List<Actividad> findByProfesorMateriaCursoIdAndProfesorMateriaMateriaId(
            @Param("cursoId") Long cursoId,
            @Param("materiaId") Long materiaId);

    @Query("SELECT a FROM Actividad a LEFT JOIN FETCH a.preguntas p LEFT JOIN FETCH p.opciones WHERE a.id = :id")
    Optional<Actividad> findByIdWithPreguntasAndOpciones(@Param("id") Long id);
}