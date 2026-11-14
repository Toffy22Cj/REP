package com.rep.dto.actividad;

import lombok.Data;

@Data
public class ResultadoPreguntaDTO {
    private Long preguntaId;
    private boolean esCorrecta;
    private String retroalimentacion;
    private String enunciado;
    /** Lo que respondió el estudiante (texto de la opción o respuesta abierta). */
    private String respuestaEstudiante;
    /** Texto de la opción correcta; nulo si el profesor no publica las respuestas. */
    private String respuestaCorrecta;
}