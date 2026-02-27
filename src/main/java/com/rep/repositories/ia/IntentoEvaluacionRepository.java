package com.rep.repositories.ia;

import com.rep.model.ia.IntentoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntentoEvaluacionRepository extends JpaRepository<IntentoEvaluacion, Long> {
    List<IntentoEvaluacion> findByEstudianteId(Long estudianteId);

    List<IntentoEvaluacion> findByPreguntaId(Long preguntaId);
}
