package com.rep.controller.apis;

import com.rep.dto.actividad.ActividadConPreguntasDTO;
import com.rep.dto.actividad.ActividadCreateDTO;
import com.rep.dto.actividad.ActividadDTO;
import com.rep.model.*;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.service.logica.ActividadService;
import com.rep.service.logica.EstudianteService;
import com.rep.service.logica.ValidacionService;
import com.rep.exception.AccessDeniedException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/actividades")
public class ActividadApi {

    private final ActividadService actividadService;
    private final ValidacionService validacionService;
    private final EstudianteService estudianteService;
    private final ProfesorMateriaRepository profesorMateriaRepository;

    public ActividadApi(ActividadService actividadService,
                        ValidacionService validacionService,
                        EstudianteService estudianteService,
                        ProfesorMateriaRepository profesorMateriaRepository) {
        this.actividadService = actividadService;
        this.validacionService = validacionService;
        this.estudianteService = estudianteService;
        this.profesorMateriaRepository = profesorMateriaRepository;
    }

    // 1. Creación de actividades
    @PostMapping
    public ResponseEntity<ActividadDTO> crearActividad(
            @RequestBody @Valid ActividadCreateDTO actividadDTO,
            @AuthenticationPrincipal Usuario usuario) {
        Profesor profesor = exigirProfesor(usuario);
        validarFechasYDuracion(actividadDTO);

        // La relación profesor-materia-curso la crea el administrador. Antes, si no
        // existía, se creaba aquí automáticamente: un profesor podía asignarse cualquier
        // curso y acceder a sus estudiantes.
        ProfesorMateria pm = profesorMateriaRepository
                .findByProfesorIdAndMateriaIdAndCursoId(profesor.getId(), actividadDTO.getMateriaId(),
                        actividadDTO.getCursoId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No tiene asignada esa materia en ese curso. Solicite la asignación al administrador"));

        Actividad actividad = new Actividad();
        actividad.setTitulo(actividadDTO.getTitulo());
        actividad.setTipo(actividadDTO.getTipo());
        actividad.setDescripcion(actividadDTO.getDescripcion());
        actividad.setFechaEntrega(actividadDTO.getFechaEntrega());
        actividad.setDuracionMinutos(actividadDTO.getDuracionMinutos());
        actividad.setProfesorMateria(pm);
        actividad.setProfesor(profesor);
        actividad.setFechaCreacion(LocalDateTime.now());
        actividad.setActiva(true);
        actividad.setPermitirReintentos(false);

        Actividad nuevaActividad = actividadService.crearActividad(actividad);
        log.info("Actividad {} creada por profesor {}", nuevaActividad.getId(), profesor.getId());
        return ResponseEntity.ok(ActividadDTO.fromEntity(nuevaActividad));
    }

    private static Profesor exigirProfesor(Usuario usuario) {
        if (usuario instanceof Profesor profesor) {
            return profesor;
        }
        throw new AccessDeniedException("Solo los profesores pueden gestionar actividades");
    }

    private static void validarFechasYDuracion(ActividadCreateDTO dto) {
        if (dto.getDuracionMinutos() == null || dto.getDuracionMinutos() <= 0) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "La duración debe ser mayor a 0",
                    Map.of("duracionMinutos", "Debe ser mayor a 0"));
        }
        if (dto.getFechaEntrega() != null && dto.getFechaEntrega().isBefore(LocalDate.now())) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "La fecha de entrega no puede ser en el pasado",
                    Map.of("fechaEntrega", "No puede ser en el pasado"));
        }
    }

    @GetMapping
    public ResponseEntity<List<ActividadDTO>> listarActividades(
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Long cursoId,
            @AuthenticationPrincipal Usuario usuario) {

        exigirProfesor(usuario);
        List<ActividadDTO> dtos = actividadService.listarActividadesPorProfesor(materiaId, cursoId, usuario.getId())
                .stream()
                .map(ActividadDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 3. Obtener una actividad específica
    @GetMapping("/{id}")
    public ResponseEntity<ActividadDTO> obtenerActividad(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        Actividad actividad = actividadService.getActividadById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ACTIVIDAD_NO_ENCONTRADA, id));
        return ResponseEntity.ok(ActividadDTO.fromEntity(actividad));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadDTO> actualizarActividad(
            @PathVariable Long id,
            @RequestBody @Valid ActividadCreateDTO actividadDTO,
            @AuthenticationPrincipal Usuario usuario) {
        Profesor profesor = exigirProfesor(usuario);
        validarFechasYDuracion(actividadDTO);

        Actividad actividadExistente = actividadService.getActividadById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ACTIVIDAD_NO_ENCONTRADA, id));
        if (actividadExistente.getProfesor() == null || !actividadExistente.getProfesor().getId().equals(profesor.getId())) {
            throw new AccessDeniedException("No tiene permisos para actualizar esta actividad");
        }

        actividadExistente.setTitulo(actividadDTO.getTitulo());
        actividadExistente.setTipo(actividadDTO.getTipo());
        actividadExistente.setDescripcion(actividadDTO.getDescripcion());
        actividadExistente.setFechaEntrega(actividadDTO.getFechaEntrega());
        actividadExistente.setDuracionMinutos(actividadDTO.getDuracionMinutos());

        return ResponseEntity.ok(ActividadDTO.fromEntity(actividadService.editarActividad(id, actividadExistente)));
    }

    // 5. Eliminar actividad
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarActividad(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        actividadService.eliminarActividad(id);
        return ResponseEntity.noContent().build();
    }

    // 5b. Publicar / ocultar una actividad a los estudiantes (las inactivas no cuentan en las notas)
    @PutMapping("/{id}/estado")
    public ResponseEntity<ActividadDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activa,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        Actividad actividad = actividadService.getActividadById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ACTIVIDAD_NO_ENCONTRADA, id));
        actividad.setActiva(activa);
        return ResponseEntity.ok(ActividadDTO.fromEntity(actividadService.editarActividad(id, actividad)));
    }

    // 6. Estadísticas de actividad
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        Map<String, Object> estadisticas = actividadService.getEstadisticasActividad(id);
        return ResponseEntity.ok(estadisticas);
    }

    // 7. Dashboard del profesor (podría moverse a ProfesorApi si prefieres)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @AuthenticationPrincipal Usuario usuario) {
        Map<String, Object> dashboardData = actividadService.getDashboardData(usuario.getId());
        return ResponseEntity.ok(dashboardData);
    }

    // 8. Clonar actividad
    @PostMapping("/{id}/clonar")
    public ResponseEntity<ActividadDTO> clonarActividad(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        Actividad actividadClonada = actividadService.clonarActividad(id);
        return ResponseEntity.ok(ActividadDTO.fromEntity(actividadClonada));
    }

    // 9. Buscar actividades por título
    @GetMapping("/buscar")
    public ResponseEntity<List<ActividadDTO>> buscarPorTitulo(
            @RequestParam String titulo,
            @AuthenticationPrincipal Usuario usuario) {
        List<Actividad> actividades = actividadService.buscarPorTituloYProfesor(titulo, usuario.getId());
        List<ActividadDTO> dtos = actividades.stream()
                .map(ActividadDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // 10. Gestión de Calificaciones (Resultados)
    @GetMapping("/{id}/resultados")
    public ResponseEntity<List<com.rep.dto.actividad.ResultadoActividadDTO>> obtenerResultados(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        // Validar que el profesor sea dueño de la actividad
        validacionService.validarProfesorActividad(usuario.getId(), id);
        return ResponseEntity.ok(actividadService.obtenerResultados(id));
    }

    @GetMapping("/{id}/respuestas/{estudianteId}")
    public ResponseEntity<com.rep.dto.actividad.RespuestaEstudianteDetalleDTO> obtenerRespuestaEstudiante(
            @PathVariable Long id,
            @PathVariable Long estudianteId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);

        com.rep.model.RespuestaEstudiante respuesta = actividadService.obtenerRespuestaEstudiante(id, estudianteId);

        // Mapear a DTO seguro para evitar problemas de serialización de proxies
        com.rep.dto.actividad.RespuestaEstudianteDetalleDTO dto = new com.rep.dto.actividad.RespuestaEstudianteDetalleDTO();
        dto.setId(respuesta.getId());
        if (respuesta.getEstudiante() != null) {
            dto.setEstudianteId(respuesta.getEstudiante().getId());
            dto.setNombreEstudiante((respuesta.getEstudiante().getNombre() + " "
                    + respuesta.getEstudiante().getApellido()).trim());
        }
        dto.setNota(respuesta.getNota());
        dto.setObservaciones(respuesta.getObservaciones());
        dto.setFechaEntrega(respuesta.getFechaEntrega());

        java.util.List<com.rep.dto.actividad.RespuestaPreguntaDetalleDTO> list = respuesta.getRespuestasPreguntas()
                .stream()
                .sorted(java.util.Comparator.comparing(rp -> rp.getPregunta() != null ? rp.getPregunta().getId() : 0L))
                .map(rp -> {
                    com.rep.dto.actividad.RespuestaPreguntaDetalleDTO rpd = new com.rep.dto.actividad.RespuestaPreguntaDetalleDTO();
                    rpd.setId(rp.getId());
                    if (rp.getPregunta() != null) {
                        rpd.setPreguntaId(rp.getPregunta().getId());
                        rpd.setEnunciado(rp.getPregunta().getEnunciado());
                    }
                    if (rp.getOpcion() != null) {
                        rpd.setOpcionId(rp.getOpcion().getId());
                        rpd.setOpcionTexto(rp.getOpcion().getTexto());
                    }
                    rpd.setRespuestaAbierta(rp.getRespuestaAbierta());
                    // No se expone la ruta interna del servidor
                    rpd.setNombreArchivo(rp.getNombreArchivo());
                    rpd.setEsCorrecta(rp.getEsCorrecta());
                    return rpd;
                }).collect(java.util.stream.Collectors.toList());

        dto.setRespuestasPreguntas(list);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/notas/{estudianteId}")
    public ResponseEntity<com.rep.model.RespuestaEstudiante> actualizarNota(
            @PathVariable Long id,
            @PathVariable Long estudianteId,
            @RequestParam Float nota,
            @RequestParam(required = false) String observaciones,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        return ResponseEntity.ok(actividadService.actualizarNota(id, estudianteId, nota, observaciones,
                usuario instanceof Profesor profesor ? profesor : null));
    }

    @GetMapping("/{id}/preguntas")
    public ResponseEntity<ActividadConPreguntasDTO> getActividadConPreguntas(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        log.info("Usuario {} (Rol: {}) consultando preguntas para actividad {}", usuario.getId(), usuario.getRol(), id);

        boolean tienePermiso = false;
        if (usuario.getRol() == Usuario.Rol.PROFESOR) {
            tienePermiso = actividadService.profesorTieneAccesoAActividad(usuario.getId(), id);
        } else if (usuario.getRol() == Usuario.Rol.ESTUDIANTE) {
            tienePermiso = estudianteService.puedeRealizarActividad(usuario.getId(), id);
            log.info("Resultado puedeRealizarActividad para estudiante {}: {}", usuario.getId(), tienePermiso);
        }

        if (!tienePermiso) {
            log.warn("Acceso DENEGADO para usuario {} a actividad {}", usuario.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ActividadConPreguntasDTO dto = actividadService.getActividadConPreguntas(id);
        if (usuario.getRol() == Usuario.Rol.ESTUDIANTE) {
            dto.ocultarSoluciones();
        }
        return ResponseEntity.ok(dto);
    }

}