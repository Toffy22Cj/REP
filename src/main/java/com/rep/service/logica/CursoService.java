package com.rep.service.logica;

import com.rep.dto.curso.CursoDTO;
import java.util.List;

// En CursoService.java
public interface CursoService {
    // Cambiar el return type de getCursosByProfesor
    List<CursoDTO> getCursosByProfesor(Long profesorId);  // Cambiado de List<Curso> a List<CursoDTO>

    // Resto de métodos permanecen igual
    CursoDTO crearCurso(CursoDTO cursoDTO);
    CursoDTO actualizarCurso(Long id, CursoDTO cursoDTO);
    void eliminarCurso(Long id);
    CursoDTO obtenerCursoPorId(Long id);
    List<CursoDTO> listarTodosLosCursos();
    List<CursoDTO> contarEstudiantesPorCurso();
    boolean profesorTieneAccesoACurso(Long profesorId, Long cursoId);
    boolean existeCurso(Long cursoId);
}