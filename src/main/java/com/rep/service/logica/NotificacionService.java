// NotificacionService.java
package com.rep.service.logica;

import com.rep.model.Notificacion;
import java.util.List;

public interface NotificacionService {
    List<Notificacion> obtenerNotificaciones(Long estudianteId, boolean soloNoLeidas);
    void marcarComoLeida(Long notificacionId, Long estudianteId);
    Notificacion crearNotificacion(Long estudianteId, String mensaje, Long actividadId);
}