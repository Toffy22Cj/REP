package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "respuestas_pregunta")
public class RespuestaPregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respuesta_estudiante_id", nullable = false)
    private RespuestaEstudiante respuestaEstudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id")
    private Opcion opcion;

    @Column(name = "respuesta_abierta")
    private String respuestaAbierta;

    @Column(name = "es_correcta")
    private Boolean esCorrecta;

    // Método helper para relación bidireccional
    public void setRespuestaEstudiante(RespuestaEstudiante respuestaEstudiante) {
        this.respuestaEstudiante = respuestaEstudiante;
        if (respuestaEstudiante != null && !respuestaEstudiante.getRespuestasPreguntas().contains(this)) {
            respuestaEstudiante.getRespuestasPreguntas().add(this);
        }
    }
}