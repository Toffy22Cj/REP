package com.rep.model.ia;

import com.rep.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "logs_actividad", catalog = "rep_ia")
public class LogActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(name = "materia_id")
    private Long materiaId;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(columnDefinition = "JSON")
    private String metadata; // Storing JSON as String for simplicity in JPA

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}
