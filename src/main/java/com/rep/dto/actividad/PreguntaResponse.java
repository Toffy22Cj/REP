package com.rep.dto.actividad;

import com.rep.model.Pregunta;
import lombok.Data;

import java.util.List;

@Data
public class PreguntaResponse {
    private Long id;
    private String enunciado;
    private Pregunta.TipoPregunta tipo;
    private List<OpcionResponse> opciones;

    // Constructor desde entidad
    public PreguntaResponse(Pregunta pregunta) {
        this.id = pregunta.getId();
        this.enunciado = pregunta.getEnunciado();
        this.tipo = pregunta.getTipo();
        this.opciones = pregunta.getOpciones().stream()
                .map(OpcionResponse::new)
                .toList();
    }

}
