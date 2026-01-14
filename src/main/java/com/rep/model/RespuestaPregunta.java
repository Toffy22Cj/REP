package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "respuestas_pregunta")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RespuestaPregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respuesta_estudiante_id", nullable = false)
    @JsonIgnore
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

    @Column(name = "archivo_adjunto")
    private String archivoAdjunto; // Ruta del archivo en el servidor

    @Column(name = "nombre_archivo")
    private String nombreArchivo; // Nombre original del archivo

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