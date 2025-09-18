package com.rep.repositories;

import com.rep.model.RespuestaEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RespuestaEstudianteRepository extends JpaRepository<RespuestaEstudiante, Long> {
    List<RespuestaEstudiante> findByActividadId(Long actividadId);
    List<RespuestaEstudiante> findByEstudianteId(Long estudianteId);

    @Query("SELECT r.actividad.id, AVG(r.nota) FROM RespuestaEstudiante r " +
            "WHERE r.actividad.profesorMateria.curso.id = ?1 " +
            "AND r.actividad.profesorMateria.materia.id = ?2 " +
            "GROUP BY r.actividad.id")
    List<Object[]> findPromediosByCursoAndMateria(Long cursoId, Long materiaId);

    @Query("SELECT r.estudiante.id, COUNT(r) FROM RespuestaEstudiante r " +
            "WHERE r.actividad.id = ?1 " +
            "GROUP BY r.estudiante.id")
    List<Object[]> findEstadoEntregasByActividad(Long actividadId);

    @Query("SELECT r FROM RespuestaEstudiante r " +
            "WHERE (:actividadId IS NULL OR r.actividad.id = :actividadId) " +
            "AND (:cursoId IS NULL OR r.actividad.profesorMateria.curso.id = :cursoId) " +
            "AND (:materiaId IS NULL OR r.actividad.profesorMateria.materia.id = :materiaId)")
    List<RespuestaEstudiante> filtrarRespuestas(
            @Param("actividadId") Long actividadId,
            @Param("cursoId") Long cursoId,
            @Param("materiaId") Long materiaId
    );

    boolean existsByActividadId(Long actividadId);

    // En RespuestaEstudianteRepository.java
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RespuestaEstudiante r " +
            "WHERE r.id = :respuestaId " +
            "AND r.actividad.profesorMateria.profesor.id = :profesorId")
    boolean existsByIdAndActividadProfesorMateriaProfesorId(
            @Param("respuestaId") Long respuestaId,
            @Param("profesorId") Long profesorId
    );

    @Query("SELECT r FROM RespuestaEstudiante r " +
            "WHERE r.actividad.profesorMateria.curso.id = :cursoId " +
            "AND r.actividad.profesorMateria.materia.id = :materiaId")
    List<RespuestaEstudiante> findByCursoIdAndMateriaId(
            @Param("cursoId") Long cursoId,
            @Param("materiaId") Long materiaId
    );

    @Query("SELECT r FROM RespuestaEstudiante r " +
            "WHERE r.actividad.id = :actividadId " +
            "AND r.actividad.profesorMateria.curso.id = :cursoId " +
            "AND r.actividad.profesorMateria.materia.id = :materiaId")
    List<RespuestaEstudiante> findByActividadIdAndCursoIdAndMateriaId(
            @Param("actividadId") Long actividadId,
            @Param("cursoId") Long cursoId,
            @Param("materiaId") Long materiaId
    );

    @Query("SELECT r FROM RespuestaEstudiante r " +
            "WHERE r.actividad.id = :actividadId " +
            "AND r.estudiante.curso.id = :cursoId")
    List<RespuestaEstudiante> findByActividadIdAndCursoId(
            @Param("actividadId") Long actividadId,
            @Param("cursoId") Long cursoId
    );
    @Query("SELECT o.texto, COUNT(r) FROM RespuestaPregunta r JOIN r.opcion o " +
            "WHERE o.pregunta.id = :preguntaId GROUP BY o.texto")
    List<Object[]> findDistribucionRespuestasByPregunta(@Param("preguntaId") Long preguntaId);
    boolean existsByEstudianteIdAndActividadId(Long estudianteId, Long actividadId);
    Optional<RespuestaEstudiante> findByEstudianteIdAndActividadId(Long estudianteId, Long actividadId);
}