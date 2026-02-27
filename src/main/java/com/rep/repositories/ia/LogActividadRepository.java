package com.rep.repositories.ia;

import com.rep.model.ia.LogActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogActividadRepository extends JpaRepository<LogActividad, Long> {
    List<LogActividad> findByEstudianteId(Long estudianteId);

    List<LogActividad> findByAccion(String accion);
}
