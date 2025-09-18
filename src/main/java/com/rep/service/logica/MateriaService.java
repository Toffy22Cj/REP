package com.rep.service.logica;

import com.rep.model.Materia;
import java.util.List;

public interface MateriaService {
    List<Materia> getMateriasByProfesor(Long profesorId);
    boolean profesorTieneAccesoAMateria(Long profesorId, Long materiaId);
    boolean existeMateria(Long materiaId);
    
}