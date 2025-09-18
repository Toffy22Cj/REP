// EstudianteApiService.java (nuevo nombre del archivo)
package com.rep.service.funciones;

import com.rep.dto.actividad.ActividadDTO;
import com.rep.dto.actividad.MateriaDTO;
import java.util.List;


public interface EstudianteApiService {
    List<MateriaDTO> getMateriasByEstudiante(Long id);
    List<ActividadDTO> getActividadesByEstudiante(Long id);
    List<ActividadDTO> getActividadesByMateria(Long id, Long materiaId);
}