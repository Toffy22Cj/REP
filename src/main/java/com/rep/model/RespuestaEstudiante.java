package com.rep.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "respuestas_estudiante")
public class RespuestaEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @DecimalMin("0.0") @DecimalMax("5.0")
    @Column
    private Float nota;

    @Column(nullable = false)
    private Boolean entregado = false;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Min(1) @Max(300)
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Size(max = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calificado_por")
    private Profesor calificadoPor;

    @OneToMany(mappedBy = "respuestaEstudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaPregunta> respuestasPreguntas = new ArrayList<>();

    // Método helper para relación bidireccional
    public void addRespuestaPregunta(RespuestaPregunta respuesta) {
        respuestasPreguntas.add(respuesta);
        respuesta.setRespuestaEstudiante(this);
    }

}