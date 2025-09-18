package com.rep.repositories;

import com.rep.model.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OpcionRepository extends JpaRepository<Opcion, Long> {
    List<Opcion> findByPreguntaId(Long preguntaId);

    @Query("SELECT COUNT(o) FROM Opcion o WHERE o.pregunta.id = :preguntaId")
    long countByPreguntaId(@Param("preguntaId") Long preguntaId);

    @Modifying
    @Query("DELETE FROM Opcion o WHERE o.pregunta.id = :preguntaId")
    void deleteByPreguntaId(@Param("preguntaId") Long preguntaId);
}