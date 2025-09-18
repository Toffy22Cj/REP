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

import java.util.List;

@RestController
@RequestMapping("/api/preguntas")
public class PreguntaApi {
    private final PreguntaService preguntaService;

    public PreguntaApi(PreguntaService preguntaService) {
        this.preguntaService = preguntaService;
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
}