package com.rep.service.impl;

import com.rep.dto.asistencia.AsistenciaDTO;
import com.rep.model.*;
import com.rep.repositories.*;
import com.rep.service.logica.AsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AsistenciaServiceImpl implements AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final EstudianteRepository estudianteRepository;

    public AsistenciaServiceImpl(AsistenciaRepository asistenciaRepository,
                                 ProfesorMateriaRepository profesorMateriaRepository,
                                 EstudianteRepository estudianteRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.estudianteRepository = estudianteRepository;
    }

    @Override
    public List<Asistencia> getAsistenciaByCursoMateriaFecha(Long profesorId, Long cursoId, Long materiaId,
            LocalDate fecha) {
        Optional<ProfesorMateria> pmOpt = profesorMateriaRepository.findByProfesorIdAndMateriaIdAndCursoId(profesorId,
                materiaId, cursoId);

        if (pmOpt.isPresent()) {
            return asistenciaRepository.findByProfesorMateriaAndFecha(pmOpt.get().getId(), fecha);
        }
        return List.of();
    }

    @Override
    @Transactional
    public List<Asistencia> saveAsistencias(Long profesorId, Long cursoId, Long materiaId, LocalDate fecha,
            List<AsistenciaDTO> asistenciaDTOs) {
        ProfesorMateria pm = profesorMateriaRepository
                .findByProfesorIdAndMateriaIdAndCursoId(profesorId, materiaId, cursoId)
                .orElseThrow(() -> new com.rep.exception.RecursoNoEncontradoException(
                        com.rep.exception.ErrorCode.ASIGNACION_NO_ENCONTRADA));

        List<Asistencia> savedAsistencias = new ArrayList<>();

        for (AsistenciaDTO dto : asistenciaDTOs) {
            Estudiante estudiante = estudianteRepository.findById(dto.getEstudianteId())
                    .orElseThrow(() -> new com.rep.exception.RecursoNoEncontradoException(com.rep.exception.ErrorCode.ESTUDIANTE_NO_ENCONTRADO, dto.getEstudianteId()));

            // Buscar si ya existe asistencia para actualizar
            Asistencia asistencia = asistenciaRepository
                    .findByProfesorMateriaIdAndEstudianteIdAndFecha(pm.getId(), estudiante.getId(), fecha)
                    .orElse(new Asistencia());

            if (asistencia.getId() == null) {
                asistencia.setProfesorMateria(pm);
                asistencia.setEstudiante(estudiante);
                asistencia.setFecha(fecha);
            }

            // Actualizar campos
            // Antes un estado desconocido se guardaba en silencio como falla (NO_INGRESO)
            try {
                asistencia.setEstado(Asistencia.EstadoAsistencia.valueOf(String.valueOf(dto.getEstado()).trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new com.rep.exception.ValidacionException(com.rep.exception.ErrorCode.CAMPOS_INVALIDOS,
                        "Estado de asistencia no válido: " + dto.getEstado(),
                        java.util.Map.of("estado", "Use PRESENTE, RETARDO, EXCUSA o NO_INGRESO"));
            }
            asistencia.setTipoExcusa(dto.getTipoExcusa());
            asistencia.setObservacion(dto.getObservacion());

            savedAsistencias.add(asistenciaRepository.save(asistencia));
        }

        return savedAsistencias;
    }

    @Override
    public List<LocalDate> getFechasConAsistencia(Long profesorId, Long cursoId, Long materiaId) {
        Optional<ProfesorMateria> pmOpt = profesorMateriaRepository.findByProfesorIdAndMateriaIdAndCursoId(profesorId,
                materiaId, cursoId);

        if (pmOpt.isPresent()) {
            return asistenciaRepository.findFechasByProfesorMateriaId(pmOpt.get().getId());
        }
        return List.of();
    }
}
