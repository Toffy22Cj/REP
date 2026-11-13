package com.rep.dto.actividad;

import com.rep.model.Opcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Opción vista por el profesor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcionResponse {
    private Long id;
    private String texto;
    private Boolean esCorrecta;
    private boolean archivoDisponible;
    private String nombreArchivo;
    private String archivoUrl;

    public OpcionResponse(Opcion opcion) {
        this.id = opcion.getId();
        this.texto = opcion.getTexto();
        this.esCorrecta = opcion.getEsCorrecta();
        if (opcion.getArchivoUrl() != null && !opcion.getArchivoUrl().isBlank()) {
            this.archivoDisponible = true;
            this.nombreArchivo = opcion.getNombreArchivo() != null
                    ? opcion.getNombreArchivo()
                    : new java.io.File(opcion.getArchivoUrl()).getName();
            this.archivoUrl = "/api/preguntas/opciones/" + opcion.getId() + "/archivo";
        }
    }
}
