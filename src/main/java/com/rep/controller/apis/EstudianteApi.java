package com.rep.controller.apis;

import com.rep.dto.actividad.ActividadDTO;
import com.rep.dto.actividad.ActividadResueltaDTO;
import com.rep.dto.actividad.MateriaDTO;
import com.rep.dto.actividad.ResultadoActividadDTO;
import com.rep.exception.AccessDeniedException;
import com.rep.model.Actividad;
import com.rep.model.Usuario;
import com.rep.service.logica.EstudianteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API del portal del estudiante.
 *
 * <p>Todas las rutas incluyen el id del estudiante en el path (contrato usado por el
 * cliente de escritorio). Para evitar accesos a datos de otros estudiantes (IDOR),
 * cada operación verifica que el usuario autenticado sea ese estudiante, salvo los
 * administradores, que pueden consultar a cualquiera.</p>
 *
 * <p>Los errores se delegan en {@link com.rep.exception.GlobalExceptionHandler}, que
 * produce respuestas JSON homogéneas (404, 400, 403...).</p>
 */
@RestController
@RequestMapping("/api/estudiante")
public class EstudianteApi {
    private static final Logger logger = LoggerFactory.getLogger(EstudianteApi.class);

    private final EstudianteService estudianteService;
    private final com.rep.service.logica.NotasService notasService;
    private final com.rep.service.archivos.NotasReportGenerator reportes;

    public EstudianteApi(EstudianteService estudianteService, com.rep.service.logica.NotasService notasService,
            com.rep.service.archivos.NotasReportGenerator reportes) {
        this.estudianteService = estudianteService;
        this.notasService = notasService;
        this.reportes = reportes;
    }

    /** Boletín de notas: promedio y desempeño por materia, estado de cada actividad y asistencia. */
    @GetMapping("/{id}/notas")
    public com.rep.dto.notas.NotasDTOs.Boletin notas(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        verificarAcceso(id, usuario);
        return notasService.boletin(id);
    }

    /** Boletín en PDF (para guardar o imprimir sin conexión). */
    @GetMapping(value = "/{id}/notas/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> notasPdf(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        verificarAcceso(id, usuario);
        var boletin = notasService.boletin(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("boletin-" + boletin.identificacion() + ".pdf").build().toString())
                .body(reportes.boletinPdf(boletin));
    }

    @GetMapping("/{id}/materias")
    public ResponseEntity<List<MateriaDTO>> getMateriasByEstudiante(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        verificarAcceso(id, usuario);
        logger.debug("Solicitud de materias para estudiante {}", id);

        List<MateriaDTO> response = estudianteService.getMateriasByEstudiante(id).stream()
                .map(m -> new MateriaDTO(m.getId(), m.getNombre()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/actividades")
    public ResponseEntity<List<ActividadDTO>> getActividadesByEstudiante(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        verificarAcceso(id, usuario);
        logger.debug("Solicitud de actividades para estudiante {}", id);

        return ResponseEntity.ok(toDtos(estudianteService.getActividadesByEstudiante(id)));
    }

    @GetMapping("/{id}/materias/{materiaId}/actividades")
    public ResponseEntity<List<ActividadDTO>> getActividadesByMateria(
            @PathVariable Long id,
            @PathVariable Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {
        verificarAcceso(id, usuario);
        logger.debug("Solicitud de actividades para estudiante {} y materia {}", id, materiaId);

        return ResponseEntity.ok(toDtos(estudianteService.getActividadesByMateria(id, materiaId)));
    }

    @PostMapping("/{id}/actividades/{actividadId}/resolver")
    public ResponseEntity<ResultadoActividadDTO> resolverActividad(
            @PathVariable Long id,
            @PathVariable Long actividadId,
            @RequestBody ActividadResueltaDTO request,
            @AuthenticationPrincipal Usuario usuario) {
        // Solo el propio estudiante puede entregar su actividad (ni siquiera un admin).
        if (usuario == null || !id.equals(usuario.getId())) {
            throw new AccessDeniedException("Solo el propio estudiante puede resolver la actividad");
        }
        logger.info("Estudiante {} resolviendo actividad {}", id, actividadId);
        return ResponseEntity.ok(estudianteService.resolverActividad(id, actividadId, request));
    }

    @GetMapping("/{id}/actividades/{actividadId}/resultado")
    public ResponseEntity<ResultadoActividadDTO> obtenerResultado(
            @PathVariable Long id,
            @PathVariable Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        verificarAcceso(id, usuario);
        logger.debug("Consultando resultado de actividad {} para estudiante {}", actividadId, id);
        return ResponseEntity.ok(estudianteService.obtenerResultadoActividad(id, actividadId));
    }

    /**
     * Permite el acceso si el usuario autenticado es el estudiante solicitado o un
     * administrador.
     */
    private void verificarAcceso(Long estudianteId, Usuario usuario) {
        if (usuario == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        boolean esPropietario = estudianteId.equals(usuario.getId());
        boolean esAdmin = usuario.getRol() == Usuario.Rol.ADMIN;
        if (!esPropietario && !esAdmin) {
            logger.warn("Usuario {} intentó acceder a datos del estudiante {}", usuario.getId(), estudianteId);
            throw new AccessDeniedException("No tiene acceso a los datos de este estudiante");
        }
    }

    private List<ActividadDTO> toDtos(List<Actividad> actividades) {
        return actividades.stream().map(EstudianteApi::toDto).toList();
    }

    private static ActividadDTO toDto(Actividad actividad) {
        var pm = actividad.getProfesorMateria();
        return new ActividadDTO(
                actividad.getId(),
                actividad.getDuracionMinutos(),
                actividad.getTitulo(),
                actividad.getTipo(),
                actividad.getDescripcion(),
                actividad.getFechaEntrega(),
                actividad.getFechaCreacion(),
                pm.getProfesor().getId(),
                pm.getMateria().getId(),
                pm.getMateria().getNombre(),
                pm.getCurso().getId(),
                pm.getCurso().getNombre(),
                actividad.getActiva());
    }
}
