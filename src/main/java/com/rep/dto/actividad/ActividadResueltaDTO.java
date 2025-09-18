package com.rep.dto.actividad;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActividadResueltaDTO {
    @NotNull
    private Long actividadId;

    @NotNull
    private Long estudianteId;
    private LocalDateTime fechaEnvio;
    private List<RespuestaPreguntaDTO> respuestas;
    private Integer duracionMinutos;
    private String observaciones;
}