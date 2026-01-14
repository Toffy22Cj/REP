package com.rep.dto.actividad;

import com.rep.model.Pregunta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PreguntaResponse {
    private Long id;
    private String enunciado;
    private Pregunta.TipoPregunta tipo;
    private List<OpcionResponse> opciones;
    private String archivoUrl; // Ruta del archivo adjunto
    private String archivoTipo; // Tipo MIME del archivo

    // Constructor desde entidad
    public PreguntaResponse(Pregunta pregunta) {
        this.id = pregunta.getId();
        this.enunciado = pregunta.getEnunciado();
        this.tipo = pregunta.getTipo();
        this.archivoUrl = pregunta.getArchivoUrl();
        this.archivoTipo = pregunta.getArchivoTipo();
        this.opciones = pregunta.getOpciones().stream()
                .map(OpcionResponse::new)
                .toList();
    }

}
