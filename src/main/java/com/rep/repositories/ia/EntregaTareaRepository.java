package com.rep.repositories.ia;

import com.rep.model.ia.EntregaTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Long> {
    List<EntregaTarea> findByEstudianteId(Long estudianteId);

    List<EntregaTarea> findByMateriaId(Long materiaId);

    List<EntregaTarea> findByEstudianteIdAndMateriaId(Long estudianteId, Long materiaId);
}
