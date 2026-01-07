package com.rep.service.logica;

import com.rep.dto.asistencia.AsistenciaDTO;
import com.rep.model.Asistencia;
import java.time.LocalDate;
import java.util.List;

public interface AsistenciaService {
    List<Asistencia> getAsistenciaByCursoMateriaFecha(Long profesorId, Long cursoId, Long materiaId, LocalDate fecha);

    // Using a wrapper or just passing the context fields
    List<Asistencia> saveAsistencias(Long profesorId, Long cursoId, Long materiaId, LocalDate fecha,
            List<AsistenciaDTO> asistenciaDTOs);

    List<LocalDate> getFechasConAsistencia(Long profesorId, Long cursoId, Long materiaId);
}
