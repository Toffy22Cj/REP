package com.rep.service.impl;

import com.rep.dto.actividad.ActividadConPreguntasDTO;
import com.rep.dto.actividad.OpcionDTO;
import com.rep.dto.actividad.PreguntaConOpcionesDTO;
import com.rep.model.Actividad;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.ProfesorMateria;
import com.rep.repositories.ActividadRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.RespuestaEstudianteRepository;
import com.rep.service.logica.ActividadService;
import com.rep.exception.RecursoNoEncontradoException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Slf4j
@Service
public class ActividadServiceImpl implements ActividadService {


    private final ActividadRepository actividadRepository;
    private final RespuestaEstudianteRepository respuestaEstudianteRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;

    public ActividadServiceImpl(ActividadRepository actividadRepository,
                                RespuestaEstudianteRepository respuestaEstudianteRepository,
                                ProfesorMateriaRepository profesorMateriaRepository) {
        this.actividadRepository = actividadRepository;
        this.respuestaEstudianteRepository = respuestaEstudianteRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
    }
    @Override
    public Actividad crearActividad(Actividad actividad) {
        try {
            // Validación básica
            if (actividad.getTitulo() == null || actividad.getTitulo().trim().isEmpty()) {
                throw new IllegalArgumentException("El título de la actividad no puede estar vacío");
            }

            // Validar relación Profesor-Materia
            ProfesorMateria pm = actividad.getProfesorMateria();
            if (pm == null || pm.getId() == null) {
                throw new IllegalArgumentException("La actividad debe estar asociada a una relación Profesor-Materia válida");
            }

            // Verificar existencia de la relación
            ProfesorMateria relacionExistente = profesorMateriaRepository.findById(pm.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe la relación Profesor-Materia con ID: " + pm.getId()));

            // Validar coherencia entre la relación y el profesor asignado
            if (!relacionExistente.getProfesor().getId().equals(actividad.getProfesor().getId())) {
                throw new IllegalArgumentException("El profesor de la actividad no coincide con la relación Profesor-Materia");
            }

            // Establecer fechas
            actividad.setFechaCreacion(LocalDateTime.now());

            // Guardar actividad
            Actividad actividadGuardada = actividadRepository.save(actividad);
            log.info("Actividad creada con ID: {}", actividadGuardada.getId());

            return actividadGuardada;

        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad de datos al crear actividad", e);
            throw new IllegalArgumentException("Error de datos: " + e.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public Actividad editarActividad(Long id, Actividad actividad) {
        Actividad existente = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));
        existente.setTitulo(actividad.getTitulo());
        existente.setTipo(actividad.getTipo());
        existente.setDescripcion(actividad.getDescripcion());
        existente.setFechaEntrega(actividad.getFechaEntrega());
        return actividadRepository.save(existente);
    }

    @Override
    public void eliminarActividad(Long id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        if (respuestaEstudianteRepository.existsByActividadId(id)) {
            throw new IllegalStateException("No se puede eliminar: La actividad tiene respuestas asociadas");
        }
        actividadRepository.deleteById(id);
    }


    @Override
    public List<Actividad> listarActividades(Long materiaId, Long cursoId) {
        if (materiaId != null && cursoId != null) {
            return actividadRepository.findByProfesorMateriaMateriaIdAndProfesorMateriaCursoId(materiaId, cursoId);
        } else if (materiaId != null) {
            return actividadRepository.findByProfesorMateriaMateriaId(materiaId);
        } else if (cursoId != null) {
            return actividadRepository.findByProfesorMateriaCursoId(cursoId);
        }
        return actividadRepository.findAll();
    }

    @Override
    public List<Actividad> listarActividadesPorProfesor(Long materiaId, Long cursoId, Long profesorId) {
        if (materiaId != null && cursoId != null) {
            return actividadRepository.findByProfesorMateriaMateriaIdAndProfesorMateriaCursoIdAndProfesorId(
                    materiaId, cursoId, profesorId);
        } else if (materiaId != null) {
            return actividadRepository.findByProfesorMateriaMateriaIdAndProfesorId(materiaId, profesorId);
        } else if (cursoId != null) {
            return actividadRepository.findByProfesorMateriaCursoIdAndProfesorId(cursoId, profesorId);
        }
        return actividadRepository.findByProfesorId(profesorId);
    }
    @Override
    public Optional<Actividad> getActividadById(Long id) {
        return actividadRepository.findById(id);
    }
    @Override
    public List<Object[]> obtenerDistribucionRespuestas(Long preguntaId) {
        // Implementación similar a PreguntaService o ajustada para Actividad
        return respuestaEstudianteRepository.findDistribucionRespuestasByPregunta(preguntaId);
    }

    @Override
    public boolean profesorTieneAccesoAActividad(Long profesorId, Long actividadId) {
        log.debug("Validando acceso para profesor {} a actividad {}", profesorId, actividadId);

        // Opción 1: Más eficiente (una sola consulta)
        boolean tieneAcceso = actividadRepository.existsByIdAndProfesorMateria_Profesor_Id(actividadId, profesorId);

        // Opción 2: Más explícita (dos consultas)
        // boolean tieneAcceso = actividadRepository.findProfesorIdByActividadId(actividadId)
        //         .map(id -> id.equals(profesorId))
        //         .orElse(false);

        log.debug("Resultado validación: {}", tieneAcceso);
        return tieneAcceso;
    }


    @Override
    public boolean existeActividad(Long actividadId) {
        return actividadRepository.existsById(actividadId);
    }

    @Override
    public Map<String, Object> getEstadisticasActividad(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        List<Object[]> entregas = respuestaEstudianteRepository.findEstadoEntregasByActividad(actividadId);
        long totalEntregas = entregas.stream().mapToLong(arr -> (Long) arr[1]).sum();

        return Map.of(
                "totalEstudiantes", actividad.getProfesorMateria().getCurso().getEstudiantes().size(),
                "entregasRealizadas", totalEntregas,
                "porcentajeEntregas", totalEntregas * 100.0 / actividad.getProfesorMateria().getCurso().getEstudiantes().size()
        );
    }

    @Override
    public Map<String, Object> getDashboardData(Long profesorId) {
        long totalActividades = actividadRepository.countByProfesorMateriaProfesorId(profesorId);
        long actividadesRecientes = actividadRepository.countByProfesorMateriaProfesorIdAndFechaCreacionAfter(
                profesorId,
                LocalDateTime.now().minusDays(7)
        );

        return Map.of(
                "totalActividades", totalActividades,
                "actividadesRecientes", actividadesRecientes,
                "cursosAsignados", profesorMateriaRepository.countDistinctCursoByProfesorId(profesorId)
        );
    }
    @Override
    public List<Actividad> buscarPorTituloYProfesor(String titulo, Long profesorId) {
        return actividadRepository.findByTituloAndProfesorId(titulo, profesorId);
    }

    @Override
    public ActividadConPreguntasDTO getActividadConPreguntas(Long actividadId) {
        Actividad actividad = actividadRepository.findByIdWithPreguntasAndOpciones(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        // Verifica que las preguntas y opciones se cargaron
        if(actividad.getPreguntas() != null) {
            log.debug("Actividad tiene {} preguntas", actividad.getPreguntas().size());
            actividad.getPreguntas().forEach(p -> {
                if(p.getOpciones() != null) {
                    log.debug("Pregunta {} tiene {} opciones", p.getId(), p.getOpciones().size());
                }
            });
        }

        return mapToDto(actividad);
    }

    private ActividadConPreguntasDTO mapToDto(Actividad actividad) {
        ActividadConPreguntasDTO dto = new ActividadConPreguntasDTO();
        dto.setId(actividad.getId());
        dto.setTitulo(actividad.getTitulo());
        dto.setDescripcion(actividad.getDescripcion());

        dto.setPreguntas(actividad.getPreguntas().stream()
                .map(this::mapPreguntaToDto)
                .toList());

        return dto;
    }

    private PreguntaConOpcionesDTO mapPreguntaToDto(Pregunta pregunta) {
        PreguntaConOpcionesDTO preguntaDto = new PreguntaConOpcionesDTO();
        preguntaDto.setId(pregunta.getId());
        preguntaDto.setEnunciado(pregunta.getEnunciado());
        preguntaDto.setTipo(pregunta.getTipo());

        preguntaDto.setOpciones(pregunta.getOpciones().stream()
                .map(this::mapOpcionToDto)
                .toList());

        return preguntaDto;
    }

    private OpcionDTO mapOpcionToDto(Opcion opcion) {
        OpcionDTO opcionDto = new OpcionDTO();
        opcionDto.setId(opcion.getId());
        opcionDto.setTexto(opcion.getTexto());
        opcionDto.setEsCorrecta(opcion.getEsCorrecta());
        return opcionDto;
    }

}
