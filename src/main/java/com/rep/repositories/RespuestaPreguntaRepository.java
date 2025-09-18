package com.rep.repositories;

import com.rep.model.RespuestaPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RespuestaPreguntaRepository extends JpaRepository<RespuestaPregunta, Long> {
    boolean existsByPreguntaId(Long preguntaId);

    @Query("SELECT COUNT(r) FROM RespuestaPregunta r WHERE r.pregunta.id = :preguntaId")
    long countByPreguntaId(@Param("preguntaId") Long preguntaId);

    @Query("SELECT o.texto, COUNT(r) FROM RespuestaPregunta r JOIN r.opcion o " +
            "WHERE o.pregunta.id = :preguntaId GROUP BY o.texto")
    List<Object[]> contarRespuestasPorOpcion(@Param("preguntaId") Long preguntaId);

    @Query("SELECT r.opcion, COUNT(r) FROM RespuestaPregunta r " +
            "WHERE r.pregunta.id = :preguntaId AND r.opcion IS NOT NULL " +
            "GROUP BY r.opcion")
    List<Object[]> contarRespuestasPorOpcionCompleta(@Param("preguntaId") Long preguntaId);
    @Query("SELECT COUNT(r) FROM RespuestaPregunta r " +
            "WHERE r.pregunta.id = :preguntaId AND r.esCorrecta = true")
    long countByPreguntaIdAndEsCorrectaTrue(@Param("preguntaId") Long preguntaId);

    @Query("SELECT COUNT(r) FROM RespuestaPregunta r " +
            "WHERE r.respuestaEstudiante.id = :respuestaEstudianteId AND r.esCorrecta = true")
    long countCorrectasByRespuestaEstudianteId(@Param("respuestaEstudianteId") Long respuestaEstudianteId);

    @Query("SELECT COUNT(r) FROM RespuestaPregunta r " +
            "WHERE r.respuestaEstudiante.id = :respuestaEstudianteId")
    long countByRespuestaEstudianteId(@Param("respuestaEstudianteId") Long respuestaEstudianteId);

    List<RespuestaPregunta> findByRespuestaEstudianteId(Long respuestaEstudianteId);
}