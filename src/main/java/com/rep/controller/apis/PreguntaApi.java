package com.rep.controller.apis;

import com.rep.dto.actividad.*;
import com.rep.exception.AccessDeniedException;
import com.rep.exception.ErrorCode;
import com.rep.exception.ValidacionException;
import com.rep.model.Actividad;
import com.rep.model.Estudiante;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.Usuario;
import com.rep.service.logica.EstudianteService;
import com.rep.service.logica.PreguntaService;
import com.rep.service.logica.ValidacionService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Preguntas de las actividades y sus adjuntos (imágenes, PDF, documentos, audio, vídeo).
 *
 * <p>Gestión: solo el profesor dueño de la actividad. Descarga de adjuntos: el profesor
 * dueño o los estudiantes del curso de la actividad.</p>
 */
@RestController
@RequestMapping("/api/preguntas")
public class PreguntaApi {
    private final PreguntaService preguntaService;
    private final EstudianteService estudianteService;
    private final ValidacionService validacionService;

    public PreguntaApi(PreguntaService preguntaService, EstudianteService estudianteService,
            ValidacionService validacionService) {
        this.preguntaService = preguntaService;
        this.estudianteService = estudianteService;
        this.validacionService = validacionService;
    }

    @PostMapping
    public PreguntaResponse crearPregunta(@Valid @RequestBody PreguntaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), request.getActividadId());
        return new PreguntaResponse(preguntaService.crearPregunta(request));
    }

    @GetMapping("/{id}")
    public PreguntaResponse getPreguntaById(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), id);
        return new PreguntaResponse(preguntaService.getPreguntaById(id));
    }

    @GetMapping("/actividad/{actividadId}")
    public List<PreguntaResponse> getPreguntasByActividad(@PathVariable Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), actividadId);
        return preguntaService.getPreguntasByActividad(actividadId).stream().map(PreguntaResponse::new).toList();
    }

    @PutMapping("/{id}")
    public PreguntaResponse actualizarPregunta(@PathVariable Long id, @Valid @RequestBody PreguntaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), id);
        return new PreguntaResponse(preguntaService.actualizarPregunta(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPregunta(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), id);
        preguntaService.eliminarPregunta(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{preguntaId}/opciones")
    public OpcionResponse agregarOpcion(@PathVariable Long preguntaId, @Valid @RequestBody OpcionRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), preguntaId);
        return new OpcionResponse(preguntaService.agregarOpcion(preguntaId, request));
    }

    // ------------------------------------------------------------------ adjuntos

    @PostMapping("/{id}/archivo")
    public PreguntaResponse subirArchivo(@PathVariable Long id, @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), id);
        return new PreguntaResponse(preguntaService.subirArchivo(id, file));
    }

    @DeleteMapping("/{id}/archivo")
    public ResponseEntity<Void> eliminarArchivo(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), id);
        preguntaService.eliminarArchivo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{preguntaId}/opciones/{opcionId}/archivo")
    public OpcionResponse subirArchivoOpcion(@PathVariable Long preguntaId, @PathVariable Long opcionId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), preguntaId);
        exigirOpcionDePregunta(preguntaService.getOpcionById(opcionId), preguntaId);
        return new OpcionResponse(preguntaService.subirArchivoOpcion(opcionId, file));
    }

    /** Variante por posición de la opción (útil al crear varias opciones desde el editor). */
    @PostMapping("/{preguntaId}/opciones/archivo")
    public OpcionResponse subirArchivoOpcionPorIndice(@PathVariable Long preguntaId,
            @RequestParam("opcionIndex") Integer opcionIndex, @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorPregunta(usuario.getId(), preguntaId);
        List<Opcion> opciones = preguntaService.getOpcionesByPreguntaId(preguntaId).stream()
                .sorted(java.util.Comparator.comparing(Opcion::getId)).toList();
        if (opcionIndex == null || opcionIndex < 0 || opcionIndex >= opciones.size()) {
            throw new ValidacionException(ErrorCode.PARAMETRO_INVALIDO, "Índice de opción fuera de rango");
        }
        return new OpcionResponse(preguntaService.subirArchivoOpcion(opciones.get(opcionIndex).getId(), file));
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> obtenerArchivo(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        exigirAccesoLectura(usuario, preguntaService.getPreguntaById(id));
        return preguntaService.descargarArchivo(id);
    }

    @GetMapping("/opciones/{opcionId}/archivo")
    public ResponseEntity<Resource> obtenerArchivoOpcion(@PathVariable Long opcionId,
            @AuthenticationPrincipal Usuario usuario) {
        exigirAccesoLectura(usuario, preguntaService.getOpcionById(opcionId).getPregunta());
        return preguntaService.descargarArchivoOpcion(opcionId);
    }

    // ------------------------------------------------------------------ control de acceso

    private void exigirAccesoLectura(Usuario usuario, Pregunta pregunta) {
        if (usuario.getRol() == Usuario.Rol.ADMIN) {
            return;
        }
        if (usuario.getRol() == Usuario.Rol.PROFESOR) {
            if (!preguntaService.profesorTieneAccesoAPregunta(usuario.getId(), pregunta.getId())) {
                throw new AccessDeniedException("No tiene acceso a esta pregunta");
            }
            return;
        }
        // Estudiante: debe pertenecer al curso de la actividad
        Actividad actividad = pregunta.getActividad();
        Long cursoActividad = actividad.getProfesorMateria() != null && actividad.getProfesorMateria().getCurso() != null
                ? actividad.getProfesorMateria().getCurso().getId() : null;
        Estudiante estudiante = estudianteService.getEstudianteById(usuario.getId());
        if (cursoActividad == null || estudiante.getCurso() == null || !cursoActividad.equals(estudiante.getCurso().getId())) {
            throw new AccessDeniedException("No tiene acceso a esta pregunta");
        }
    }

    private static void exigirOpcionDePregunta(Opcion opcion, Long preguntaId) {
        // Sin esta comprobación un profesor podría modificar opciones de preguntas ajenas
        if (opcion.getPregunta() == null || !preguntaId.equals(opcion.getPregunta().getId())) {
            throw new AccessDeniedException(ErrorCode.OPCION_NO_PERTENECE_A_PREGUNTA);
        }
    }
}
