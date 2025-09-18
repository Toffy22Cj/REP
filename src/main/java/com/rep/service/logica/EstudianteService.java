// EstudianteService.java
package com.rep.service.logica;

import com.rep.dto.actividad.ActividadResueltaDTO;
import com.rep.dto.actividad.ResultadoActividadDTO;
import com.rep.model.*;

import java.util.List;

public interface EstudianteService {

    List<Estudiante> getEstudiantesByCurso(Long cursoId);
    Estudiante cambiarEstadoEstudiante(Long id, String estado);
    Estudiante getEstudianteById(Long id);
    String exportEstudiantesByCursoToCsv(Long cursoId);
    boolean profesorTieneAccesoAEstudiante(Long profesorId, Long estudianteId);
    boolean existeEstudiante(Long estudianteId);

    // Calificaciones
    List<Calificacion> getCalificacionesByEstudiante(Long estudianteId);
    Calificacion agregarCalificacion(Long estudianteId, Long actividadId, Double puntuacion, String comentarios);
    Double getPromedioCalificaciones(Long estudianteId);

    // Notificaciones
//    List<Notificacion> getNotificacionesByEstudiante(Long estudianteId, boolean soloNoLeidas);
    void marcarNotificacionComoLeida(Long notificacionId);
    // Recursos
    List<Recurso> getRecursosByCursoDelEstudiante(Long estudianteId);
    List<Materia> getMateriasByEstudiante(Long estudianteId);
    List<Actividad> getActividadesByEstudiante(Long estudianteId);
    List<Actividad> getActividadesByMateria(Long estudianteId, Long materiaId);
    // Métodos para resolución de actividades
    ResultadoActividadDTO resolverActividad(Long estudianteId, Long actividadId, ActividadResueltaDTO request);
    ResultadoActividadDTO obtenerResultadoActividad(Long estudianteId, Long actividadId);

    // Métodos adicionales que podrían ser necesarios
    List<Pregunta> getPreguntasByActividad(Long actividadId);
    boolean puedeRealizarActividad(Long estudianteId, Long actividadId);
}