// ActividadService.java
package com.rep.service.logica;

import com.rep.dto.actividad.ActividadConPreguntasDTO;
import com.rep.model.Actividad;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ActividadService {
    List<Actividad> buscarPorTituloYProfesor(String titulo, Long profesorId);
    Actividad crearActividad(Actividad actividad);
    Actividad editarActividad(Long id, Actividad actividad);
    void eliminarActividad(Long id);
    List<Actividad> listarActividades(Long materiaId, Long cursoId);
    Optional<Actividad> getActividadById(Long id);
    boolean profesorTieneAccesoAActividad(Long profesorId, Long actividadId);
    boolean existeActividad(Long actividadId);
    Map<String, Object> getEstadisticasActividad(Long actividadId);
    Map<String, Object> getDashboardData(Long profesorId);
    List<Object[]> obtenerDistribucionRespuestas(Long preguntaId);

    List<Actividad> listarActividadesPorProfesor(Long materiaId, Long cursoId, Long profesorId);
    ActividadConPreguntasDTO getActividadConPreguntas(Long actividadId);
}