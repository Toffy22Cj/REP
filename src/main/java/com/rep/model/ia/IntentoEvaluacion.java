package com.rep.model.ia;

import com.rep.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "intentos_evaluacion", catalog = "rep_ia")
public class IntentoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    // TODO: Link to Pregunta entity when available in main model
    @Column(name = "pregunta_id", nullable = false)
    private Long preguntaId;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "respuesta_texto", columnDefinition = "TEXT")
    private String respuestaTexto;

    @Column(name = "respuesta_correcta", columnDefinition = "TEXT")
    private String respuestaCorrecta;

    @Column(name = "similitud_semantica", precision = 5, scale = 4)
    private BigDecimal similitudSemantica;

    @Column(name = "es_correcta")
    private Boolean esCorrecta;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_evaluacion")
    private MetodoEvaluacion metodoEvaluacion;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;

    public enum MetodoEvaluacion {
        EXACTA, SEMANTICA, PARCIAL, MANUAL
    }
}
