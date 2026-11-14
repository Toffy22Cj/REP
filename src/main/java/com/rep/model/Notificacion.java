package com.rep.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
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

    @JdbcTypeCode(SqlTypes.VARCHAR) // VARCHAR y no ENUM nativo: añadir valores no rompe MySQL
    private TipoNotificacion tipo;

    private String mensaje;
    private boolean leida = false;
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
