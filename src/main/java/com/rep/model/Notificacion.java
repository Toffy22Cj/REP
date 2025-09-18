package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity
@Table(name = "notificaciones")
@Data
public class Notificacion {
    public enum TipoNotificacion {
        NUEVA_ACTIVIDAD,
        CALIFICACION,
        MENSAJE,
        RECORDATORIO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    private Actividad actividad; // Opcional

    @Enumerated(EnumType.STRING)
    private TipoNotificacion tipo;

    private String mensaje;
    private boolean leida = false;
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
