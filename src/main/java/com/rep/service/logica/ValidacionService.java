package com.rep.service.logica;

import com.rep.exception.AccessDeniedException;
import com.rep.exception.ResourceNotFoundException;
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

        // Añadir logs para diagnóstico
        System.out.println("Validando acceso para profesor: " + profesorId + ", curso: " + cursoId);

        if (!cursoService.profesorTieneAccesoACurso(profesorId, cursoId)) {
            // Más información en el mensaje de error
            throw new AccessDeniedException(
                    String.format("El profesor %d no tiene acceso al curso %d", profesorId, cursoId)
            );
        }
    }

    public void validarProfesorActividad(Long profesorId, Long actividadId) {
        if (profesorId == null || actividadId == null) {
            throw new IllegalArgumentException("ID de profesor o actividad no puede ser nulo");
        }

        log.info("Validando acceso para profesor {} a actividad {}", profesorId, actividadId);

        if (!actividadService.existeActividad(actividadId)) {
            log.error("Actividad {} no encontrada", actividadId);
            throw new ResourceNotFoundException("Actividad no encontrada");
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
            throw new ResourceNotFoundException("Estudiante no encontrado");
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
            throw new ResourceNotFoundException("Pregunta no encontrada");
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
            throw new ResourceNotFoundException("Respuesta no encontrada");
        }
        if (!respuestaService.profesorTieneAccesoARespuesta(profesorId, respuestaId)) {
            throw new AccessDeniedException("No tiene acceso a esta respuesta");
        }
    }

    /**
     * Valida que un profesor tenga acceso a una combinación específica de materia y curso
     * (Orden de parámetros: profesorId, materiaId, cursoId)
     */
    public void validarProfesorMateriaCurso(Long profesorId, Long materiaId, Long cursoId) {
        System.out.println("Validando relación: Profesor=" + profesorId +
                ", Materia=" + materiaId + ", Curso=" + cursoId);

        // Verificar existencia básica
        if (!materiaService.existeMateria(materiaId)) {
            throw new ResourceNotFoundException("Materia no existe");
        }
        if (!cursoService.existeCurso(cursoId)) {
            throw new ResourceNotFoundException("Curso no existe");
        }

        // Verificar relación profesor-materia-curso
        boolean existeRelacion = profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(
                profesorId, materiaId, cursoId);

        System.out.println("Resultado validación: " + existeRelacion);

        if (!existeRelacion) {
            throw new AccessDeniedException("El profesor no está asignado a esta materia y curso");
        }
    }

    /**
     * Valida que un profesor tenga acceso a una combinación específica de curso y materia
     * (Orden de parámetros: profesorId, cursoId, materiaId)
     */
    public void validarProfesorCursoMateria(Long profesorId, Long cursoId, Long materiaId) {
        // Validación básica de parámetros
        if (profesorId == null || cursoId == null || materiaId == null) {
            throw new IllegalArgumentException("IDs no pueden ser nulos");
        }

        // Verificar existencia de recursos
        if (!materiaService.existeMateria(materiaId)) {
            throw new ResourceNotFoundException("Materia no encontrada");
        }

        if (!cursoService.existeCurso(cursoId)) {
            throw new ResourceNotFoundException("Curso no encontrado");
        }

        // Validar relación completa (el orden de materiaId y cursoId no importa para la consulta SQL)
        if (!profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(profesorId, materiaId, cursoId)) {
            throw new AccessDeniedException(
                    String.format("El profesor %d no tiene acceso a la combinación curso %d - materia %d",
                            profesorId, cursoId, materiaId)
            );
        }
    }
//    public void validarProfesorOpcion(Long profesorId, Long opcionId) {
//        if (!preguntaService.profesorTieneAccesoAOpcion(profesorId, opcionId)) {
//            throw new AccessDeniedException("No tiene acceso a esta opción");
//        }
//    }
}