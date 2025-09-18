// RespuestaService.java
package com.rep.service.logica;

import com.rep.model.RespuestaEstudiante;
import com.rep.model.RespuestaPregunta;
import java.util.List;

public interface RespuestaService {
    byte[] generarReporteResultados(Long cursoId, Long actividadId, String formato);
    List<RespuestaEstudiante> getRespuestasByActividad(Long actividadId);
    List<RespuestaEstudiante> getRespuestasByEstudiante(Long estudianteId);
    RespuestaEstudiante calificarRespuesta(Long id, Float nota);
    List<Object[]> getPromediosByCursoMateria(Long cursoId, Long materiaId);
    List<Object[]> getEstadoEntregasByActividad(Long actividadId);
    RespuestaPregunta guardarRespuestaPregunta(RespuestaPregunta respuestaPregunta);
    boolean profesorTieneAccesoARespuesta(Long profesorId, Long respuestaId);
    boolean existeRespuesta(Long respuestaId);
    byte[] generatePromediosPdfReport(Long cursoId, Long materiaId);
    List<RespuestaEstudiante> filtrarRespuestas(Long actividadId, Long cursoId, Long materiaId);
}