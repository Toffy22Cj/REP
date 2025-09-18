package com.rep.service.logica;

import com.rep.model.ProfesorMateria;
import java.util.List;
import java.util.Optional;

public interface ProfesorMateriaService {

    ProfesorMateria guardarProfesorMateria(ProfesorMateria profesorMateria);
    Optional<ProfesorMateria> findByProfesorIdAndMateriaIdAndCursoId(Long profesorId, Long materiaId, Long cursoId);
    List<ProfesorMateria> findByProfesorId(Long profesorId);
    List<ProfesorMateria> findByMateriaId(Long materiaId);
    List<ProfesorMateria> findByCursoId(Long cursoId);
    boolean existsByProfesorIdAndMateriaIdAndCursoId(Long profesorId, Long materiaId, Long cursoId);
    boolean existsByProfesorIdAndCursoId(Long profesorId, Long cursoId);
    long countDistinctCursoByProfesorId(Long profesorId);

    // Nuevo método declarado (la implementación va en la clase)
    List<ProfesorMateria> findByCursoIdAndMateriaId(Long cursoId, Long materiaId);
}