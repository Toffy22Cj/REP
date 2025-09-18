package com.rep.dto.actividad;

import lombok.Data;

import java.util.List;

@Data
public class ResultadoActividadDTO {
    private Long actividadId;
    private Long estudianteId;
    private Float nota;
    private List<ResultadoPreguntaDTO> resultadosPreguntas;
    private String observaciones;
}
