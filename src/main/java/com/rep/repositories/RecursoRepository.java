package com.rep.repositories;

import com.rep.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    List<Recurso> findByProfesorMateriaCursoId(Long cursoId);
}