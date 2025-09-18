// NotificacionServiceImpl.java
package com.rep.service.impl;

import com.rep.model.*;
import com.rep.repositories.*;
import com.rep.service.logica.NotificacionService;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;

@Service
public class NotificacionServiceImpl implements NotificacionService {
    private final NotificacionRepository notificacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ActividadRepository actividadRepository;

    public NotificacionServiceImpl(
            NotificacionRepository notificacionRepository,
            EstudianteRepository estudianteRepository,
            ActividadRepository actividadRepository) {
        this.notificacionRepository = notificacionRepository;
        this.estudianteRepository = estudianteRepository;
        this.actividadRepository = actividadRepository;
    }

    @Override
    public List<Notificacion> obtenerNotificaciones(Long estudianteId, boolean soloNoLeidas) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new EntityNotFoundException("Estudiante no encontrado");
        }
        return soloNoLeidas ?
                notificacionRepository.findByEstudianteIdAndLeidaFalse(estudianteId) :
                notificacionRepository.findByEstudianteIdOrderByFechaCreacionDesc(estudianteId);
    }

    @Override
    public void marcarComoLeida(Long notificacionId, Long estudianteId) {
        int updated = notificacionRepository.marcarComoLeida(notificacionId, estudianteId);
        if (updated == 0) {
            throw new EntityNotFoundException("Notificación no encontrada o no pertenece al estudiante");
        }
    }

    @Override
    public Notificacion crearNotificacion(Long estudianteId, String mensaje, Long actividadId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        Notificacion notificacion = new Notificacion();
        notificacion.setEstudiante(estudiante);
        notificacion.setMensaje(mensaje);

        if (actividadId != null) {
            Actividad actividad = actividadRepository.findById(actividadId)
                    .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));
            notificacion.setActividad(actividad);
            notificacion.setTipo(Notificacion.TipoNotificacion.NUEVA_ACTIVIDAD);
        } else {
            notificacion.setTipo(Notificacion.TipoNotificacion.MENSAJE);
        }

        return notificacionRepository.save(notificacion);
    }
}