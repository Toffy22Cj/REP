package com.rep.repositories;

import com.rep.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    List<Calificacion> findByEstudianteId(Long estudianteId);
    List<Calificacion> findByActividadId(Long actividadId);
    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.estudiante.id = :estudianteId")
    Double calcularPromedioByEstudianteId(@Param("estudianteId") Long estudianteId);
}