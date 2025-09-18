package com.rep.dto.actividad;

import com.rep.model.Pregunta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PreguntaRequest {
    @NotNull(message = "El ID de actividad es requerido")
    private Long actividadId;

    @NotBlank(message = "El enunciado no puede estar vacío")
    @Size(max = 500, message = "El enunciado no puede exceder 500 caracteres")
    private String enunciado;

    @NotNull(message = "El tipo de pregunta es requerido")
    private Pregunta.TipoPregunta tipo;

    @Valid
    private List<OpcionRequest> opciones;
}
