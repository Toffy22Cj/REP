package com.rep.dto.actividad;

import com.rep.model.Pregunta.TipoPregunta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
@Data
public class PreguntaPlantillaDTO {
    @NotNull
    private Long actividadId;

    @NotBlank
    @Size(max = 500)
    private String enunciado;

    @NotNull
    private TipoPregunta tipo;

    @Valid // Valida también las opciones
    private List<OpcionPlantillaDTO> opciones;
}