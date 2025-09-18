package com.rep.dto.actividad;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespuestaPreguntaDTO {
    @NotNull
    private Long preguntaId;
    private Long opcionId; // Para opción múltiple/Verdadero-Falso
    private String respuestaAbierta; // Para respuesta abierta
}