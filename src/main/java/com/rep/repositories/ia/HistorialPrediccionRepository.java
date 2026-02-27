package com.rep.repositories.ia;

import com.rep.model.ia.HistorialPrediccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialPrediccionRepository extends JpaRepository<HistorialPrediccion, Long> {
    List<HistorialPrediccion> findByEstudianteId(Long estudianteId);

    List<HistorialPrediccion> findByEstudianteIdAndTipoPrediccion(Long estudianteId, String tipoPrediccion);
}
