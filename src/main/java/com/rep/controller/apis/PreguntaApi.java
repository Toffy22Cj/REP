package com.rep.controller.apis;

import com.rep.dto.actividad.*;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.Usuario;
import com.rep.service.logica.PreguntaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/preguntas")
    public class PreguntaApi {
    private static final Logger logger = LoggerFactory.getLogger(PreguntaApi.class);
    private final PreguntaService preguntaService;
    private final com.rep.service.logica.EstudianteService estudianteService;

    public PreguntaApi(PreguntaService preguntaService, com.rep.service.logica.EstudianteService estudianteService) {
        this.preguntaService = preguntaService;
        this.estudianteService = estudianteService;
    }

    // 1. Crear pregunta (con opciones si es necesario)
    @PostMapping
    @PreAuthorize("@validacionService.validarProfesorActividad(#usuario.id, #request.actividadId)")
    public ResponseEntity<PreguntaResponse> crearPregunta(
            @Valid @RequestBody PreguntaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Pregunta pregunta = preguntaService.crearPregunta(request);
        return ResponseEntity.ok(new PreguntaResponse(pregunta));
    }

    // 2. Obtener pregunta por ID
    @GetMapping("/{id}")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #id)")
    public ResponseEntity<PreguntaResponse> getPreguntaById(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        Pregunta pregunta = preguntaService.getPreguntaById(id);
        return ResponseEntity.ok(new PreguntaResponse(pregunta));
    }

    // 3. Listar preguntas por actividad
    @GetMapping("/actividad/{actividadId}")
    @PreAuthorize("@validacionService.validarProfesorActividad(#usuario.id, #actividadId)")
    public ResponseEntity<List<PreguntaResponse>> getPreguntasByActividad(
            @PathVariable Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        List<Pregunta> preguntas = preguntaService.getPreguntasByActividad(actividadId);
        return ResponseEntity.ok(preguntas.stream().map(PreguntaResponse::new).toList());
    }

    // 4. Actualizar pregunta
    @PutMapping("/{id}")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #id)")
    public ResponseEntity<PreguntaResponse> actualizarPregunta(
            @PathVariable Long id,
            @Valid @RequestBody PreguntaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Pregunta pregunta = preguntaService.actualizarPregunta(id, request);
        return ResponseEntity.ok(new PreguntaResponse(pregunta));
    }

    // 5. Eliminar pregunta
    @DeleteMapping("/{id}")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #id)")
    public ResponseEntity<Void> eliminarPregunta(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        preguntaService.eliminarPregunta(id);
        return ResponseEntity.noContent().build();
    }

    // 6. Agregar opción a pregunta existente
    @PostMapping("/{preguntaId}/opciones")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #preguntaId)")
    public ResponseEntity<OpcionResponse> agregarOpcion(
            @PathVariable Long preguntaId,
            @Valid @RequestBody OpcionRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Opcion opcion = preguntaService.agregarOpcion(preguntaId, request);
        return ResponseEntity.ok(new OpcionResponse(opcion));
    }

    @PostMapping("/{id}/archivo")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #id)")
    public ResponseEntity<PreguntaResponse> subirArchivo(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Pregunta pregunta = preguntaService.subirArchivo(id, file);
            return ResponseEntity.ok(new PreguntaResponse(pregunta));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Subir archivo para una opción existente
    @PostMapping("/{preguntaId}/opciones/{opcionId}/archivo")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #preguntaId)")
    public ResponseEntity<OpcionResponse> subirArchivoOpcion(
            @PathVariable Long preguntaId,
            @PathVariable Long opcionId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Opcion opcion = preguntaService.subirArchivoOpcion(opcionId, file);
            return ResponseEntity.ok(new OpcionResponse(opcion));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Subir archivo para una opción recien creada identificada por índice (útil desde el editor cuando se crean múltiples opciones)
    @PostMapping("/{preguntaId}/opciones/archivo")
    @PreAuthorize("@validacionService.validarProfesorPregunta(#usuario.id, #preguntaId)")
    public ResponseEntity<Void> subirArchivoOpcionPorIndice(
            @PathVariable Long preguntaId,
            @RequestParam("opcionIndex") Integer opcionIndex,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            List<Opcion> opciones = preguntaService.getOpcionesByPreguntaId(preguntaId);
            if (opcionIndex == null || opcionIndex < 0 || opcionIndex >= opciones.size()) {
                return ResponseEntity.badRequest().build();
            }
            Opcion target = opciones.get(opcionIndex);
            preguntaService.subirArchivoOpcion(target.getId(), file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Obtener archivo de una opción
    @GetMapping("/opciones/{opcionId}/archivo")
    public ResponseEntity<org.springframework.core.io.Resource> obtenerArchivoOpcion(
            @PathVariable Long opcionId,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            // Validar acceso: permitir profesor propietario de la pregunta o estudiante del curso
            if (usuario == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            Opcion opcion = preguntaService.getOpcionById(opcionId);
            Long preguntaId = opcion.getPregunta().getId();

            if (usuario.getRol() == Usuario.Rol.PROFESOR) {
                if (!preguntaService.profesorTieneAccesoAPregunta(usuario.getId(), preguntaId)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            } else if (usuario.getRol() == Usuario.Rol.ESTUDIANTE) {
                Pregunta pregunta = preguntaService.getPreguntaById(preguntaId);
                Long actividadId = pregunta.getActividad().getId();
                if (!estudianteService.puedeRealizarActividad(usuario.getId(), actividadId)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            java.io.File file = preguntaService.obtenerArchivoOpcion(opcionId);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toURI());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + file.getName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<org.springframework.core.io.Resource> obtenerArchivo(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            logger.info("Solicitud de archivo para pregunta id={} por usuario={}", id, usuario == null ? "ANON" : usuario.getId());
            // Validaciones: permitir profesor propietario o estudiante que puede realizar la actividad
            if (usuario == null) {
                logger.warn("Usuario no autenticado intentando acceder al archivo de pregunta {}", id);
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            if (usuario.getRol() == Usuario.Rol.PROFESOR) {
                if (!preguntaService.profesorTieneAccesoAPregunta(usuario.getId(), id)) {
                    logger.warn("Profesor {} no tiene acceso a la pregunta {}", usuario.getId(), id);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            } else if (usuario.getRol() == Usuario.Rol.ESTUDIANTE) {
                // Permitir descarga si el estudiante pertenece al curso de la actividad
                Pregunta pregunta = preguntaService.getPreguntaById(id);
                Long actividadId = pregunta.getActividad().getId();
                try {
                    com.rep.model.Estudiante estudiante = estudianteService.getEstudianteById(usuario.getId());
                    Long cursoActividadId = null;
                    if (pregunta.getActividad().getProfesorMateria() != null && pregunta.getActividad().getProfesorMateria().getCurso() != null) {
                        cursoActividadId = pregunta.getActividad().getProfesorMateria().getCurso().getId();
                    } else if (pregunta.getActividad().getCurso() != null) {
                        cursoActividadId = pregunta.getActividad().getCurso().getId();
                    }

                    if (cursoActividadId == null || estudiante.getCurso() == null || !cursoActividadId.equals(estudiante.getCurso().getId())) {
                        logger.warn("Estudiante {} no pertenece al curso de la actividad {} (pregunta {})", usuario.getId(), actividadId, id);
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                    }
                } catch (Exception ex) {
                    logger.warn("Error validando acceso del estudiante {} a la actividad {}: {}", usuario.getId(), actividadId, ex.getMessage());
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            java.io.File file = preguntaService.obtenerArchivo(id);
            logger.info("Archivo encontrado en ruta={}", file.getAbsolutePath());
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toURI());

            if (resource.exists() || resource.isReadable()) {
                logger.info("Enviando recurso archivo pregunta {} (size={} bytes)", id, file.length());
                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + file.getName() + "\"")
                        .body(resource);
            } else {
                logger.error("Recurso no legible para archivo pregunta {} ruta={}", id, file.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (java.io.IOException e) {
            logger.error("Error al obtener archivo para pregunta {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}