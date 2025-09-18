package com.rep.service.funciones;

import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.model.*;

import java.util.List;

public interface AdminApiService {
    // Gestión de Cursos
    List<Curso> listarCursos();
    Curso obtenerCursoPorId(Long id);
    Curso registrarCurso(Curso curso);
    Curso actualizarCurso(Long id, Curso curso);
    void eliminarCurso(Long id);
    List<Estudiante> obtenerEstudiantesPorCurso(Long cursoId);

    // Gestión de Materias
    Materia actualizarMateria(Long id, Materia materia);
    List<Materia> listarMaterias();
    Materia obtenerMateriaPorId(Long id);
    Materia crearMateria(Materia materia);
    void eliminarMateria(Long id);

    // Gestión de Usuarios
    List<Usuario> listarUsuariosPorRol(String rol);
    Usuario obtenerUsuarioPorId(Long id);
    Usuario actualizarUsuario(Long id, Usuario usuario);
    void eliminarUsuario(Long id);

    // Gestión de Estudiantes
    void asignarCursoAEstudiante(Long estudianteId, Long cursoId);

    // Gestión de Profesores
    Profesor obtenerProfesorPorId(Long id);
    Profesor actualizarEstadoProfesor(Long id, String estado);

    // Gestión de Asignaciones
    List<ProfesorMateria> listarAsignaciones(Long cursoId, Long materiaId);
    List<ProfesorMateria> getAsignacionesPorProfesor(Long profesorId);
    List<Materia> getMateriasPorProfesor(Long profesorId);
    ProfesorMateria crearAsignacion(ProfesorMateriaRequest request);
    void eliminarAsignacion(Long id);
}