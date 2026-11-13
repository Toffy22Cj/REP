package com.rep.dto.actividad;

import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

/** Pregunta vista por el profesor (incluye qué opción es la correcta). */
@Data
@NoArgsConstructor
public class PreguntaResponse {
    private Long id;
    private String enunciado;
    private Pregunta.TipoPregunta tipo;
    private List<OpcionResponse> opciones;
    private boolean archivoDisponible;
    /** URL de descarga en la API (nunca la ruta interna del servidor). */
    private String archivoUrl;
    private String archivoTipo;
    private String nombreArchivo;
    private boolean archivoEsImagen;

    public PreguntaResponse(Pregunta pregunta) {
        this.id = pregunta.getId();
        this.enunciado = pregunta.getEnunciado();
        this.tipo = pregunta.getTipo();
        this.opciones = pregunta.getOpciones().stream()
                .sorted(Comparator.comparing(Opcion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(OpcionResponse::new)
                .toList();
        if (pregunta.getArchivoUrl() != null && !pregunta.getArchivoUrl().isBlank()) {
            this.archivoDisponible = true;
            this.archivoUrl = "/api/preguntas/" + pregunta.getId() + "/archivo";
            this.archivoTipo = pregunta.getArchivoTipo();
            this.nombreArchivo = pregunta.getNombreArchivo() != null
                    ? pregunta.getNombreArchivo()
                    : new java.io.File(pregunta.getArchivoUrl()).getName();
            this.archivoEsImagen = archivoTipo != null && archivoTipo.startsWith("image/");
        }
    }
}
