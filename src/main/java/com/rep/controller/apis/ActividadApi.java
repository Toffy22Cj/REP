package com.rep.controller.apis;

import com.rep.dto.actividad.ActividadCreateDTO;
import com.rep.dto.actividad.ActividadDTO;
import com.rep.model.Actividad;
import com.rep.model.Profesor;
import com.rep.model.ProfesorMateria;
import com.rep.model.Usuario;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.service.logica.ActividadService;
import com.rep.service.logica.ValidacionService;
import com.rep.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@RestController
@RequestMapping("/api/actividades")
public class ActividadApi {

    @Autowired
    private ActividadService actividadService;

    @Autowired
    private ValidacionService validacionService;
    @Autowired
    ProfesorMateriaRepository profesorMateriaRepository;


    // 1. Creación de actividades
    @PostMapping
    public ResponseEntity<?> crearActividad(
            @RequestBody @Valid ActividadCreateDTO actividadDTO,
            BindingResult result,
            @AuthenticationPrincipal Usuario usuario) {

        try {
            // 1. Validar usuario
            if (!(usuario instanceof Profesor)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Solo los profesores pueden crear actividades");
            }

            // 2. Validar errores de binding
            if (result.hasErrors()) {
                Map<String, String> errores = result.getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                FieldError::getField,
                                FieldError::getDefaultMessage
                        ));
                return ResponseEntity.badRequest().body(errores);
            }

            // 3. Validaciones adicionales
            if (actividadDTO.getDuracionMinutos() <= 0) {
                return ResponseEntity.badRequest()
                        .body("La duración debe ser mayor a 0");
            }

            if (actividadDTO.getFechaEntrega().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body("La fecha de entrega no puede ser en el pasado");
            }

            // 4. Verificar existencia de la relación Profesor-Materia
            ProfesorMateria pm = profesorMateriaRepository.findById(actividadDTO.getProfesorMateriaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró la relación Profesor-Materia con ID: " + actividadDTO.getProfesorMateriaId()));

            // 5. Validar que el profesor autenticado es el dueño de la relación
            if (!pm.getProfesor().getId().equals(usuario.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tiene permisos para crear actividades en esta materia-curso");
            }

            // 6. Crear entidad Actividad
            Actividad actividad = new Actividad();
            actividad.setTitulo(actividadDTO.getTitulo());
            actividad.setTipo(actividadDTO.getTipo());
            actividad.setDescripcion(actividadDTO.getDescripcion());
            actividad.setFechaEntrega(actividadDTO.getFechaEntrega());
            actividad.setDuracionMinutos(actividadDTO.getDuracionMinutos());
            actividad.setProfesorMateria(pm);
            actividad.setProfesor((Profesor) usuario);

            // 7. Guardar actividad
            Actividad nuevaActividad = actividadService.crearActividad(actividad);
            return ResponseEntity.ok(ActividadDTO.fromEntity(nuevaActividad));

        } catch (ResourceNotFoundException e) {
            log.error("Error al crear actividad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al crear actividad", e);
            return ResponseEntity.internalServerError()
                    .body("Error interno al crear la actividad: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ActividadDTO>> listarActividades(
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Long cursoId,
            @AuthenticationPrincipal Usuario usuario) {

        if (!(usuario instanceof Profesor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            List<Actividad> actividades = actividadService.listarActividadesPorProfesor(
                    materiaId,
                    cursoId,
                    usuario.getId());

            List<ActividadDTO> dtos = actividades.stream()
                    .map(ActividadDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error al listar actividades", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 3. Obtener una actividad específica
    @GetMapping("/{id}")
    public ResponseEntity<ActividadDTO> obtenerActividad(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        Actividad actividad = actividadService.getActividadById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada"));
        return ResponseEntity.ok(ActividadDTO.fromEntity(actividad));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarActividad(
            @PathVariable Long id,
            @RequestBody @Valid ActividadCreateDTO actividadDTO,
            BindingResult result,
            @AuthenticationPrincipal Usuario usuario) {

        try {
            // 1. Validar usuario
            if (!(usuario instanceof Profesor)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Solo los profesores pueden actualizar actividades");
            }

            // 2. Validar errores de binding
            if (result.hasErrors()) {
                Map<String, String> errores = result.getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                FieldError::getField,
                                FieldError::getDefaultMessage
                        ));
                return ResponseEntity.badRequest().body(errores);
            }

            // 3. Validaciones adicionales
            if (actividadDTO.getDuracionMinutos() <= 0) {
                return ResponseEntity.badRequest()
                        .body("La duración debe ser mayor a 0");
            }

            if (actividadDTO.getFechaEntrega().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body("La fecha de entrega no puede ser en el pasado");
            }

            // 4. Obtener actividad existente
            Actividad actividadExistente = actividadService.getActividadById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada"));

            // 5. Validar que el profesor autenticado es el dueño de la actividad
            if (!actividadExistente.getProfesor().getId().equals(usuario.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tiene permisos para actualizar esta actividad");
            }

            // 6. Actualizar campos permitidos
            actividadExistente.setTitulo(actividadDTO.getTitulo());
            actividadExistente.setTipo(actividadDTO.getTipo());
            actividadExistente.setDescripcion(actividadDTO.getDescripcion());
            actividadExistente.setFechaEntrega(actividadDTO.getFechaEntrega());
            actividadExistente.setDuracionMinutos(actividadDTO.getDuracionMinutos());

            // 7. Guardar cambios
            Actividad actividadActualizada = actividadService.editarActividad(id, actividadExistente);
            return ResponseEntity.ok(ActividadDTO.fromEntity(actividadActualizada));

        } catch (ResourceNotFoundException e) {
            log.error("Error al actualizar actividad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al actualizar actividad", e);
            return ResponseEntity.internalServerError()
                    .body("Error interno al actualizar la actividad: " + e.getMessage());
        }
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
    public ResponseEntity<Actividad> clonarActividad(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), id);
        Actividad actividad = actividadService.getActividadById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada"));
        Actividad copia = new Actividad();
        // Copiar propiedades...
        Actividad actividadClonada = actividadService.crearActividad(copia);
        return ResponseEntity.ok(actividadClonada);
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


}