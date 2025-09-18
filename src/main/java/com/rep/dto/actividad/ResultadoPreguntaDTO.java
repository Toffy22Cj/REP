package com.rep.dto.actividad;

import lombok.Data;

@Data
public class ResultadoPreguntaDTO {
    private Long preguntaId;
    private boolean esCorrecta;
    private String retroalimentacion;
}