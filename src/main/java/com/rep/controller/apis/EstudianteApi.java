package com.rep.controller.apis;
import com.rep.dto.actividad.ActividadDTO;
import com.rep.dto.actividad.ActividadResueltaDTO;
import com.rep.dto.actividad.MateriaDTO;
import com.rep.dto.actividad.ResultadoActividadDTO;
import com.rep.dto.estudiante.NotificacionDTO;
import com.rep.model.Actividad;
import com.rep.model.Materia;
import com.rep.model.Notificacion;
import com.rep.service.logica.EstudianteService;
import com.rep.service.logica.NotificacionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController // Cambiado de @Controller a @RestController
@RequestMapping("/api/estudiante")
public class EstudianteApi {
    private static final Logger logger = LoggerFactory.getLogger(EstudianteApi.class); // Logger declarado
    private final NotificacionService notificacionService;
    private final EstudianteService estudianteService;


    public EstudianteApi(NotificacionService notificacionService, EstudianteService estudianteService) {
        this.notificacionService = notificacionService;
        this.estudianteService = estudianteService;
    }

    @GetMapping("/{id}/materias")
    public ResponseEntity<List<MateriaDTO>> getMateriasByEstudiante(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Solicitud de materias para estudiante {}", id);

        try {
            List<Materia> materias = estudianteService.getMateriasByEstudiante(id);
            List<MateriaDTO> response = materias.stream()
                    .map(m -> new MateriaDTO(m.getId(), m.getNombre()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener materias: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/actividades")
    public ResponseEntity<List<ActividadDTO>> getActividadesByEstudiante(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Solicitud de actividades para estudiante {}", id);

        try {
            List<Actividad> actividades = estudianteService.getActividadesByEstudiante(id);
            List<ActividadDTO> response = actividades.stream()
                    .map(this::convertToActividadDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener actividades: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ActividadDTO convertToActividadDTO(Actividad actividad) {
        return new ActividadDTO(
                actividad.getId(),
                actividad.getDuracionMinutos(),
                actividad.getTitulo(),
                actividad.getTipo(),
                actividad.getDescripcion(),
                actividad.getFechaEntrega(),
                actividad.getFechaCreacion(),
                actividad.getProfesorMateria().getProfesor().getId(),
                actividad.getProfesorMateria().getMateria().getId(),
                actividad.getProfesorMateria().getMateria().getNombre(),
                actividad.getProfesorMateria().getCurso().getId(),
                actividad.getProfesorMateria().getCurso().getNombre()
        );
    }

    @GetMapping("/{id}/materias/{materiaId}/actividades")
    public ResponseEntity<List<ActividadDTO>> getActividadesByMateria( // Cambiado a ActividadDTO
                                                                       @PathVariable Long id,
                                                                       @PathVariable Long materiaId,
                                                                       @RequestHeader("Authorization") String authHeader) { // Cambiado a header

        logger.info("Solicitud de actividades para estudiante {} y materia {}", id, materiaId);

        try {
            List<Actividad> actividades = estudianteService.getActividadesByMateria(id, materiaId);
            List<ActividadDTO> response = actividades.stream()
                    .map(this::convertToActividadDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener actividades por materia: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    @GetMapping("/{id}/notificaciones")
//    public ResponseEntity<List<NotificacionDTO>> getNotificaciones(
//            @PathVariable Long id,
//            @RequestParam(required = false) Boolean noLeidas,
//            @RequestHeader("Authorization") String authHeader) {
//
//        List<Notificacion> notificaciones = notificacionService.obtenerNotificaciones(
//                id,
//                noLeidas != null && noLeidas
//        );
//
//        return ResponseEntity.ok(
//                notificaciones.stream()
//                        .map(this::convertToDTO)
//                        .collect(Collectors.toList())
//        );
//    }
//
//    @PutMapping("/{id}/notificaciones/{notificacionId}/leida")
//    public ResponseEntity<Void> marcarComoLeida(
//            @PathVariable Long id,
//            @PathVariable Long notificacionId) {
//
//        notificacionService.marcarComoLeida(notificacionId, id);
//        return ResponseEntity.noContent().build();
//    }
//    private NotificacionDTO convertToDTO(Notificacion notificacion) {
//        NotificacionDTO dto = new NotificacionDTO();
//        dto.setId(notificacion.getId());
//        dto.setMensaje(notificacion.getMensaje());
//        dto.setLeida(notificacion.isLeida());
//        dto.setFechaCreacion(notificacion.getFechaCreacion());
//
//        if (notificacion.getActividad() != null) {
//            dto.setActividadId(notificacion.getActividad().getId());
//            dto.setTituloActividad(notificacion.getActividad().getTitulo());
//        }
//
//        return dto;
//    }

    @PostMapping("/{id}/actividades/{actividadId}/resolver")
    public ResponseEntity<ResultadoActividadDTO> resolverActividad(
            @PathVariable Long id,
            @PathVariable Long actividadId,
            @RequestBody ActividadResueltaDTO request,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Estudiante {} resolviendo actividad {}", id, actividadId);

        try {
            ResultadoActividadDTO resultado = estudianteService.resolverActividad(id, actividadId, request);
            return ResponseEntity.ok(resultado);
        } catch (EntityNotFoundException e) {
            logger.error("Actividad o estudiante no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error al resolver actividad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/actividades/{actividadId}/resultado")
    public ResponseEntity<ResultadoActividadDTO> obtenerResultado(
            @PathVariable Long id,
            @PathVariable Long actividadId,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Consultando resultado de actividad {} para estudiante {}", actividadId, id);

        try {
            ResultadoActividadDTO resultado = estudianteService.obtenerResultadoActividad(id, actividadId);
            return ResponseEntity.ok(resultado);
        } catch (EntityNotFoundException e) {
            logger.error("Resultado no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error al obtener resultado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}