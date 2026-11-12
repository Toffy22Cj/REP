package com.rep.service.logica;

import com.rep.exception.AccessDeniedException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.repositories.PreguntaRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ValidacionService {
    private final PreguntaRepository preguntaRepository;
    private final MateriaService materiaService;
    private final CursoService cursoService;
    private final ActividadService actividadService;
    private final EstudianteService estudianteService;
    private final PreguntaService preguntaService;
    private final RespuestaService respuestaService;
    private final ProfesorMateriaRepository profesorMateriaRepository;

    // Inyección por constructor (mejor práctica que @Autowired)
    public ValidacionService(MateriaService materiaService,
            CursoService cursoService,
            ActividadService actividadService,
            EstudianteService estudianteService,
            PreguntaService preguntaService,
            RespuestaService respuestaService,
            ProfesorMateriaRepository profesorMateriaRepository,
            PreguntaRepository preguntaRepository) {
        this.materiaService = materiaService;
        this.cursoService = cursoService;
        this.actividadService = actividadService;
        this.estudianteService = estudianteService;
        this.preguntaService = preguntaService;
        this.respuestaService = respuestaService;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.preguntaRepository = preguntaRepository;
    }

    public void validarProfesorMateria(Long profesorId, Long materiaId) {
        if (profesorId == null || materiaId == null) {
            throw new IllegalArgumentException("ID de profesor o materia no puede ser nulo");
        }
        if (!materiaService.profesorTieneAccesoAMateria(profesorId, materiaId)) {
            throw new AccessDeniedException("No tiene acceso a esta materia");
        }
    }

    public void validarProfesorCurso(Long profesorId, Long cursoId) {
        if (profesorId == null || cursoId == null) {
            throw new IllegalArgumentException("ID de profesor o curso no puede ser nulo");
        }

        log.debug("Validando acceso para profesor: {}, curso: {}", profesorId, cursoId);

        if (!cursoService.profesorTieneAccesoACurso(profesorId, cursoId)) {
            // Más información en el mensaje de error
            throw new AccessDeniedException(
                    String.format("El profesor %d no tiene acceso al curso %d", profesorId, cursoId));
        }
    }

    public void validarProfesorActividad(Long profesorId, Long actividadId) {
        if (profesorId == null || actividadId == null) {
            throw new IllegalArgumentException("ID de profesor o actividad no puede ser nulo");
        }

        log.info("Validando acceso para profesor {} a actividad {}", profesorId, actividadId);

        if (!actividadService.existeActividad(actividadId)) {
            log.error("Actividad {} no encontrada", actividadId);
            throw new RecursoNoEncontradoException(ErrorCode.ACTIVIDAD_NO_ENCONTRADA);
        }

        if (!actividadService.profesorTieneAccesoAActividad(profesorId, actividadId)) {
            log.warn("Acceso denegado: profesor {} no tiene acceso a actividad {}", profesorId, actividadId);
            throw new AccessDeniedException("No tiene acceso a esta actividad");
        }
    }

    public void validarProfesorEstudiante(Long profesorId, Long estudianteId) {
        if (profesorId == null || estudianteId == null) {
            throw new IllegalArgumentException("ID de profesor o estudiante no puede ser nulo");
        }
        if (!estudianteService.existeEstudiante(estudianteId)) {
            throw new RecursoNoEncontradoException(ErrorCode.ESTUDIANTE_NO_ENCONTRADO);
        }
        if (!estudianteService.profesorTieneAccesoAEstudiante(profesorId, estudianteId)) {
            throw new AccessDeniedException("No tiene acceso a este estudiante");
        }
    }

    public void validarProfesorPregunta(Long profesorId, Long preguntaId) {
        if (profesorId == null || preguntaId == null) {
            throw new IllegalArgumentException("ID de profesor o pregunta no puede ser nulo");
        }

        if (!preguntaRepository.existsById(preguntaId)) {
            throw new RecursoNoEncontradoException(ErrorCode.PREGUNTA_NO_ENCONTRADA);
        }

        if (!preguntaService.profesorTieneAccesoAPregunta(profesorId, preguntaId)) {
            throw new AccessDeniedException("No tiene acceso a esta pregunta");
        }
    }

    public void validarProfesorRespuesta(Long profesorId, Long respuestaId) {
        if (profesorId == null || respuestaId == null) {
            throw new IllegalArgumentException("ID de profesor o respuesta no puede ser nulo");
        }
        if (!respuestaService.existeRespuesta(respuestaId)) {
            throw new RecursoNoEncontradoException(ErrorCode.RESPUESTA_NO_ENCONTRADA);
        }
        if (!respuestaService.profesorTieneAccesoARespuesta(profesorId, respuestaId)) {
            throw new AccessDeniedException("No tiene acceso a esta respuesta");
        }
    }

    /**
     * Valida que un profesor tenga acceso a una combinación específica de materia y
     * curso
     * (Orden de parámetros: profesorId, materiaId, cursoId)
     */
    public void validarProfesorMateriaCurso(Long profesorId, Long materiaId, Long cursoId) {
        log.debug("Validando relación: Profesor={}, Materia={}, Curso={}", profesorId, materiaId, cursoId);

        // Verificar existencia básica
        if (!materiaService.existeMateria(materiaId)) {
            throw new RecursoNoEncontradoException(ErrorCode.MATERIA_NO_ENCONTRADA);
        }
        if (!cursoService.existeCurso(cursoId)) {
            throw new RecursoNoEncontradoException(ErrorCode.CURSO_NO_ENCONTRADO);
        }

        // Verificar relación profesor-materia-curso
        boolean existeRelacion = profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(
                profesorId, materiaId, cursoId);

        log.debug("Resultado validación: {}", existeRelacion);

        if (!existeRelacion) {
            throw new AccessDeniedException("El profesor no está asignado a esta materia y curso");
        }
    }

    /**
     * Valida que un profesor tenga acceso a una combinación específica de curso y
     * materia
     * (Orden de parámetros: profesorId, cursoId, materiaId)
     */
    public void validarProfesorCursoMateria(Long profesorId, Long cursoId, Long materiaId) {
        // Validación básica de parámetros
        if (profesorId == null || cursoId == null || materiaId == null) {
            throw new IllegalArgumentException("IDs no pueden ser nulos");
        }

        // Verificar existencia de recursos
        if (!materiaService.existeMateria(materiaId)) {
            throw new RecursoNoEncontradoException(ErrorCode.MATERIA_NO_ENCONTRADA);
        }

        if (!cursoService.existeCurso(cursoId)) {
            throw new RecursoNoEncontradoException(ErrorCode.CURSO_NO_ENCONTRADO);
        }

        // Validar relación completa (el orden de materiaId y cursoId no importa para la
        // consulta SQL)
        if (!profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(profesorId, materiaId, cursoId)) {
            throw new AccessDeniedException(
                    String.format("El profesor %d no tiene acceso a la combinación curso %d - materia %d",
                            profesorId, cursoId, materiaId));
        }
    }
    // public void validarProfesorOpcion(Long profesorId, Long opcionId) {
    // if (!preguntaService.profesorTieneAccesoAOpcion(profesorId, opcionId)) {
    // throw new AccessDeniedException("No tiene acceso a esta opción");
    // }
    // }
}