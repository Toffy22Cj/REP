package com.rep.dto.actividad;

import com.rep.model.Pregunta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaConOpcionesDTO {
    private Long id;
    private String enunciado;
    private Pregunta.TipoPregunta tipo;
    private List<OpcionDTO> opciones;
}
