package com.rep.dto.actividad;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RespuestaEstudianteDetalleDTO {
    private Long id;
    private Long estudianteId;
    private String nombreEstudiante;
    private Float nota;
    private String observaciones;
    private LocalDateTime fechaEntrega;
    private List<RespuestaPreguntaDetalleDTO> respuestasPreguntas;
}
