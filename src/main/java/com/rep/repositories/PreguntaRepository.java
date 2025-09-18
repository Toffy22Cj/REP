// PreguntaRepository.java
package com.rep.repositories;

import com.rep.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PreguntaRepository extends JpaRepository<Pregunta, Long>, JpaSpecificationExecutor<Pregunta> {
    List<Pregunta> findByActividadId(Long actividadId);

    List<Pregunta> findByTipoAndActividadProfesorMateriaProfesorId(
            Pregunta.TipoPregunta tipo,
            Long profesorId
    );

    @Query("SELECT o.texto, COUNT(r) FROM RespuestaPregunta r JOIN r.opcion o " +
            "WHERE o.pregunta.id = :preguntaId GROUP BY o.texto")
    List<Object[]> contarRespuestasPorOpcion(@Param("preguntaId") Long preguntaId);

    @Modifying
    @Query("DELETE FROM Pregunta p WHERE p.actividad.id = :actividadId")
    void deleteByActividadId(@Param("actividadId") Long actividadId);
    @Query("SELECT p FROM Pregunta p WHERE " +
            "LOWER(p.enunciado) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "AND p.actividad.profesorMateria.profesor.id = :profesorId")
    List<Pregunta> buscarPorTextoYProfesor(
            @Param("texto") String texto,
            @Param("profesorId") Long profesorId);
    int countByActividadId(Long actividadId);
    List<Pregunta> findByActividadIdOrderByIdAsc(Long actividadId);
    boolean existsByIdAndActividad_ProfesorMateria_Profesor_Id(
            Long preguntaId,
            Long profesorId
    );

}

