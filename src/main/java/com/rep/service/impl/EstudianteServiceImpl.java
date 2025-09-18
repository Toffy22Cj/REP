package com.rep.service.impl;

import com.rep.dto.actividad.ActividadResueltaDTO;
import com.rep.dto.actividad.RespuestaPreguntaDTO;
import com.rep.dto.actividad.ResultadoActividadDTO;
import com.rep.dto.actividad.ResultadoPreguntaDTO;
import com.rep.model.*;
import com.rep.repositories.*;
import com.rep.service.logica.CorreccionService;
import com.rep.service.logica.EstudianteService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EstudianteServiceImpl implements EstudianteService {
    private static final Logger logger = LoggerFactory.getLogger(EstudianteApiServiceImpl.class);
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final EstudianteRepository estudianteRepository;
    private final CalificacionRepository calificacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final RecursoRepository recursoRepository;
    private final ActividadRepository actividadRepository;
    private final MateriaRepository materiaRepository;
    private final RespuestaEstudianteRepository respuestaEstudianteRepository;
    private final PreguntaRepository preguntaRepository ;
    private final OpcionRepository opcionRepository;
    private final RespuestaPreguntaRepository respuestaPreguntaRepository;
    private final CorreccionService correccionService;
    public EstudianteServiceImpl(CorreccionService correccionService,RespuestaPreguntaRepository respuestaPreguntaRepository,OpcionRepository opcionRepository,PreguntaRepository preguntaRepository,
            EstudianteRepository estudianteRepository,
            ProfesorMateriaRepository profesorMateriaRepository,
            CalificacionRepository calificacionRepository,
            NotificacionRepository notificacionRepository,
            RecursoRepository recursoRepository,
            ActividadRepository actividadRepository,
            MateriaRepository materiaRepository,
            RespuestaEstudianteRepository respuestaEstudianteRepository
    ) {
        this.correccionService = correccionService;
        this.respuestaPreguntaRepository = respuestaPreguntaRepository;
        this.opcionRepository = opcionRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.calificacionRepository = calificacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.recursoRepository = recursoRepository;
        this.actividadRepository = actividadRepository;
        this.materiaRepository = materiaRepository;
        this.respuestaEstudianteRepository = respuestaEstudianteRepository;
        this.preguntaRepository = preguntaRepository;
    }
    @Override
    public List<Estudiante> getEstudiantesByCurso(Long cursoId) {
        return estudianteRepository.findByCursoId(cursoId);
    }

    @Override
    public Estudiante cambiarEstadoEstudiante(Long id, String estado) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        estudiante.setEstado(Estudiante.EstadoEstudiante.valueOf(estado));
        return estudianteRepository.save(estudiante);
    }

    @Override
    public Estudiante getEstudianteById(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
    }

    @Override
    public String exportEstudiantesByCursoToCsv(Long cursoId) {
        List<Estudiante> estudiantes = estudianteRepository.findByCursoId(cursoId);

        String[] headers = "ID,Nombre,Correo,Estado\n".split(",");
        String csv = estudiantes.stream()
                .map(e -> String.join(",",
                        e.getId().toString(),
                        "\"" + e.getNombre() + "\"",
                        e.getCorreo(),
                        e.getEstado().name()))
                .collect(Collectors.joining("\n"));

        return String.join(",", headers) + csv;
    }

    @Override
    public boolean existeEstudiante(Long id) {
        return estudianteRepository.existsById(id);
    }
    @Override
    public boolean profesorTieneAccesoAEstudiante(Long profesorId, Long estudianteId) {
        // 1. Obtener el estudiante
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        // 2. Verificar si el profesor tiene acceso al curso del estudiante
        return profesorMateriaRepository.existsByProfesorIdAndCursoId(
                profesorId,
                estudiante.getCurso().getId()
        );
    }

    @Override
    public List<Calificacion> getCalificacionesByEstudiante(Long estudianteId) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new EntityNotFoundException("Estudiante no encontrado");
        }
        return calificacionRepository.findByEstudianteId(estudianteId);
    }

    @Override
    public Calificacion agregarCalificacion(Long estudianteId, Long actividadId, Double puntuacion, String comentarios) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        Calificacion calificacion = new Calificacion();
        calificacion.setEstudiante(estudiante);
        calificacion.setActividad(actividad);
        calificacion.setPuntuacion(puntuacion);
        calificacion.setComentarios(comentarios);

        return calificacionRepository.save(calificacion);
    }

    @Override
    public Double getPromedioCalificaciones(Long estudianteId) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new EntityNotFoundException("Estudiante no encontrado");
        }
        return calificacionRepository.calcularPromedioByEstudianteId(estudianteId);
    }

//    @Override
//    public List<Notificacion> getNotificacionesByEstudiante(Long estudianteId, boolean soloNoLeidas) {
//        if (!estudianteRepository.existsById(estudianteId)) {
//            throw new EntityNotFoundException("Estudiante no encontrado");
//        }
//        return soloNoLeidas ?
//                notificacionRepository.findByEstudianteIdAndLeidaFalse(estudianteId) :
//          notificacionRepository.findByEstudianteId(estudianteId);
//    }

    @Override
    public void marcarNotificacionComoLeida(Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new EntityNotFoundException("Notificación no encontrada"));
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    @Override
    public List<Recurso> getRecursosByCursoDelEstudiante(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        return recursoRepository.findByProfesorMateriaCursoId(estudiante.getCurso().getId());
    }

    @Override
    public List<Materia> getMateriasByEstudiante(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        if (estudiante.getCurso() == null) {
            throw new IllegalStateException("El estudiante no tiene curso asignado");
        }

        List<Materia> materias = materiaRepository.findByCursoId(estudiante.getCurso().getId());

        if (materias.isEmpty()) {
            throw new IllegalStateException("El curso no tiene materias asignadas");
        }

        return materias;
    }

    @Override
    public List<Actividad> getActividadesByEstudiante(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        return actividadRepository.findByProfesorMateriaCursoId(estudiante.getCurso().getId());
    }

    @Override
    public List<Actividad> getActividadesByMateria(Long estudianteId, Long materiaId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        return actividadRepository.findByProfesorMateriaCursoIdAndProfesorMateriaMateriaId(
                estudiante.getCurso().getId(),
                materiaId
        );
    }


    @Override
    @Transactional
    public ResultadoActividadDTO resolverActividad(Long estudianteId, Long actividadId, ActividadResueltaDTO request) {
        // 1. Cargar datos necesarios con JOIN FETCH para evitar N+1
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        Actividad actividad = actividadRepository.findByIdWithPreguntasAndOpciones(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        // 2. Validación de acceso
        if (!puedeRealizarActividad(estudianteId, actividadId)) {
            throw new IllegalStateException("El estudiante no puede realizar esta actividad");
        }

        // 3. Crear o actualizar respuesta
        RespuestaEstudiante respuestaEstudiante = respuestaEstudianteRepository
                .findByEstudianteIdAndActividadId(estudianteId, actividadId)
                .orElseGet(() -> {
                    RespuestaEstudiante nueva = new RespuestaEstudiante();
                    nueva.setEstudiante(estudiante);
                    nueva.setActividad(actividad);
                    nueva.setFechaInicio(LocalDateTime.now());
                    return nueva;
                });

        // 4. Procesar respuestas
        Map<Long, Pregunta> preguntasMap = actividad.getPreguntas().stream()
                .collect(Collectors.toMap(Pregunta::getId, Function.identity()));

        for (RespuestaPreguntaDTO respuestaDTO : request.getRespuestas()) {
            Pregunta pregunta = preguntasMap.get(respuestaDTO.getPreguntaId());
            if (pregunta == null) continue;

            procesarRespuestaIndividual(respuestaEstudiante, respuestaDTO, pregunta);
        }

        // 5. Calcular y guardar
        respuestaEstudiante.setEntregado(true);
        respuestaEstudiante.setFechaEntrega(LocalDateTime.now());
        respuestaEstudiante.setNota(calcularNotaPreliminar(respuestaEstudiante, actividad));

        respuestaEstudianteRepository.save(respuestaEstudiante);
        return convertirAResultadoDTO(respuestaEstudiante, actividad);
    }

    private void procesarRespuestaIndividual(RespuestaEstudiante respuestaEstudiante,
                                             RespuestaPreguntaDTO respuestaDTO,
                                             Pregunta pregunta) {
        RespuestaPregunta respuestaPregunta = new RespuestaPregunta();
        respuestaPregunta.setPregunta(pregunta);
        respuestaPregunta.setEstudiante(respuestaEstudiante.getEstudiante());
        respuestaPregunta.setRespuestaEstudiante(respuestaEstudiante);

        if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
            respuestaPregunta.setRespuestaAbierta(respuestaDTO.getRespuestaAbierta());
            respuestaPregunta.setEsCorrecta(null);
        } else {
            Optional<Opcion> opcionCorrecta = pregunta.getOpciones().stream()
                    .filter(o -> o.getId().equals(respuestaDTO.getOpcionId()))
                    .findFirst();

            if (opcionCorrecta.isPresent()) {
                respuestaPregunta.setOpcion(opcionCorrecta.get());
                respuestaPregunta.setEsCorrecta(opcionCorrecta.get().getEsCorrecta());
            }
        }

        respuestaEstudiante.addRespuestaPregunta(respuestaPregunta);
    }

    @Override
    public ResultadoActividadDTO obtenerResultadoActividad(Long estudianteId, Long actividadId) {
        RespuestaEstudiante respuesta = respuestaEstudianteRepository
                .findByEstudianteIdAndActividadId(estudianteId, actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Respuesta no encontrada"));

        return convertirAResultadoDTO(respuesta);
    }

    @Override
    public List<Pregunta> getPreguntasByActividad(Long actividadId) {
        return preguntaRepository.findByActividadIdOrderByIdAsc(actividadId);
    }

    @Override
    public boolean puedeRealizarActividad(Long estudianteId, Long actividadId) {
        try {
            // 1. Verificar que el estudiante existe
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                    .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

            // 2. Verificar que la actividad existe
            Actividad actividad = actividadRepository.findById(actividadId)
                    .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

            // 3. Verificar que el estudiante pertenece al curso de la actividad
            if (actividad.getCurso() == null || !actividad.getCurso().getId().equals(estudiante.getCurso().getId())) {
                logger.warn("El estudiante {} no pertenece al curso de la actividad {}", estudianteId, actividadId);
                return false;
            }

            // 4. Verificar fechas
            LocalDateTime ahora = LocalDateTime.now();

            // La actividad debe haber sido creada
            if (actividad.getFechaCreacion() == null || ahora.isBefore(actividad.getFechaCreacion())) {
                logger.warn("La actividad {} no está disponible aún", actividadId);
                return false;
            }

            // Verificar fecha de entrega (si existe)
            if (actividad.getFechaHoraEntrega() != null && ahora.isAfter(actividad.getFechaHoraEntrega())) {
                logger.warn("La actividad {} ya ha pasado su fecha de entrega", actividadId);
                return false;
            }

            // 5. Verificar intentos previos
            boolean tieneIntentoPrevio = respuestaEstudianteRepository.existsByEstudianteIdAndActividadId(estudianteId, actividadId);

            if (tieneIntentoPrevio && !actividad.getPermitirReintentos()) {
                logger.warn("El estudiante {} ya ha realizado la actividad {} y no se permiten reintentos",
                        estudianteId, actividadId);
                return false;
            }

            // 6. Verificar que la actividad está activa
            if (!actividad.getActiva()) {
                logger.warn("La actividad {} no está activa", actividadId);
                return false;
            }

            // Todas las validaciones pasaron
            return true;

        } catch (EntityNotFoundException e) {
            logger.error("Error en validación: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error inesperado al validar actividad", e);
            return false;
        }
    }

//    @Override
//    public boolean puedeRealizarActividad(Estudiante estudiante, Actividad actividad) {
//        try {
//            // 1. Verificar que el estudiante pertenece al curso de la actividad
//            if (actividad.getCurso() == null || !actividad.getCurso().getId().equals(estudiante.getCurso().getId())) {
//                logger.warn("El estudiante {} no pertenece al curso de la actividad {}",
//                        estudiante.getId(), actividad.getId());
//                return false;
//            }
//
//            // 2. Verificar fechas
//            LocalDateTime ahora = LocalDateTime.now();
//
//            // La actividad debe haber sido creada
//            if (actividad.getFechaCreacion() == null || ahora.isBefore(actividad.getFechaCreacion())) {
//                logger.warn("La actividad {} no está disponible aún", actividad.getId());
//                return false;
//            }
//
//            // Verificar fecha de entrega (si existe)
//            if (actividad.getFechaHoraEntrega() != null && ahora.isAfter(actividad.getFechaHoraEntrega())) {
//                logger.warn("La actividad {} ya ha pasado su fecha de entrega", actividad.getId());
//                return false;
//            }
//
//            // 3. Verificar intentos previos
//            boolean tieneIntentoPrevio = respuestaEstudianteRepository.existsByEstudianteIdAndActividadId(
//                    estudiante.getId(), actividad.getId());
//
//            if (tieneIntentoPrevio && !actividad.getPermitirReintentos()) {
//                logger.warn("El estudiante {} ya ha realizado la actividad {} y no se permiten reintentos",
//                        estudiante.getId(), actividad.getId());
//                return false;
//            }
//
//            // 4. Verificar que la actividad está activa
//            if (!actividad.getActiva()) {
//                logger.warn("La actividad {} no está activa", actividad.getId());
//                return false;
//            }
//
//            return true;
//
//        } catch (Exception e) {
//            logger.error("Error inesperado al validar actividad", e);
//            return false;
//        }
//    }

    private float calcularNotaPreliminar(RespuestaEstudiante respuesta, Actividad actividad) {
        // Usar las preguntas ya cargadas de la actividad
        long totalPreguntasObjetivas = actividad.getPreguntas().stream()
                .filter(p -> p.getTipo() != Pregunta.TipoPregunta.RESPUESTA_ABIERTA)
                .count();

        if (totalPreguntasObjetivas == 0) return 0f;

        long respuestasCorrectas = respuesta.getRespuestasPreguntas().stream()
                .filter(rp -> rp.getEsCorrecta() != null && rp.getEsCorrecta())
                .count();

        float nota = (float) (respuestasCorrectas * 5.0 / totalPreguntasObjetivas);
        return Math.min(Math.round(nota * 10) / 10.0f, 5.0f); // Redondear a 1 decimal
    }

    // Método principal que usa la actividad de la respuesta
    private ResultadoActividadDTO convertirAResultadoDTO(RespuestaEstudiante respuesta) {
        return convertirAResultadoDTO(respuesta, respuesta.getActividad());
    }

    // Método sobrecargado para cuando ya tenemos la actividad cargada
    private ResultadoActividadDTO convertirAResultadoDTO(RespuestaEstudiante respuesta, Actividad actividad) {
        ResultadoActividadDTO dto = new ResultadoActividadDTO();
        dto.setActividadId(actividad.getId());
        dto.setEstudianteId(respuesta.getEstudiante().getId());
        dto.setNota(respuesta.getNota());
        dto.setObservaciones(respuesta.getObservaciones());

        // Usar LinkedHashSet para mantener orden
        Set<Long> preguntasProcesadas = new LinkedHashSet<>();
        List<ResultadoPreguntaDTO> resultados = new ArrayList<>();

        for (RespuestaPregunta rp : respuesta.getRespuestasPreguntas()) {
            if (preguntasProcesadas.add(rp.getPregunta().getId())) {
                resultados.add(convertirResultadoPreguntaDTO(rp));
            }
        }

        dto.setResultadosPreguntas(resultados);
        return dto;
    }

    private ResultadoPreguntaDTO convertirResultadoPreguntaDTO(RespuestaPregunta respuesta) {
        ResultadoPreguntaDTO dto = new ResultadoPreguntaDTO();
        dto.setPreguntaId(respuesta.getPregunta().getId());
        dto.setEsCorrecta(Boolean.TRUE.equals(respuesta.getEsCorrecta()));

        // Generar retroalimentación basada en el tipo de pregunta
        dto.setRetroalimentacion(generarRetroalimentacion(respuesta));

        return dto;
    }

    private String generarRetroalimentacion(RespuestaPregunta respuesta) {
        Pregunta pregunta = respuesta.getPregunta();

        switch(pregunta.getTipo()) {
            case OPCION_MULTIPLE:
            case VERDADERO_FALSO:
                if (respuesta.getOpcion() == null) {
                    return "No se seleccionó ninguna opción";
                }
                return respuesta.getEsCorrecta() ?
                        "Respuesta correcta" :
                        "Respuesta incorrecta. La opción correcta era: " +
                                pregunta.getOpciones().stream()
                                        .filter(Opcion::getEsCorrecta)
                                        .findFirst()
                                        .map(Opcion::getTexto)
                                        .orElse("No definida");

            case RESPUESTA_ABIERTA:
                return "Respuesta enviada: " +
                        (respuesta.getRespuestaAbierta() != null ?
                                respuesta.getRespuestaAbierta() : "[Vacía]") +
                        "\n(Requiere revisión manual)";

            default:
                return "Tipo de pregunta no soportado";
        }
    }
}
