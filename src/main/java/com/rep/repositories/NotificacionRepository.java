package com.rep.repositories;

import com.rep.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByEstudianteIdAndLeidaFalse(Long estudianteId);
    List<Notificacion> findByEstudianteIdOrderByFechaCreacionDesc(Long estudianteId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.id = ?1 AND n.estudiante.id = ?2")
    int marcarComoLeida(Long notificacionId, Long estudianteId);
}