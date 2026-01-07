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

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private ProfesorMateriaRepository profesorMateriaRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

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
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la asignación del profesor para este curso y materia"));

        List<Asistencia> savedAsistencias = new ArrayList<>();

        for (AsistenciaDTO dto : asistenciaDTOs) {
            Estudiante estudiante = estudianteRepository.findById(dto.getEstudianteId())
                    .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + dto.getEstudianteId()));

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
            try {
                asistencia.setEstado(Asistencia.EstadoAsistencia.valueOf(dto.getEstado()));
            } catch (IllegalArgumentException e) {
                asistencia.setEstado(Asistencia.EstadoAsistencia.NO_INGRESO); // Default or error
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
