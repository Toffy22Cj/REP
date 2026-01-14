package com.rep.dto.actividad;

import com.rep.model.Opcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Constructor desde entidad
    public OpcionResponse(Opcion opcion) {
        this.id = opcion.getId();
        this.texto = opcion.getTexto();
        this.esCorrecta = opcion.getEsCorrecta();
        if (opcion.getTieneArchivo() != null && opcion.getTieneArchivo()) {
            this.archivoDisponible = true;
            this.nombreArchivo = opcion.getNombreArchivo() != null ? opcion.getNombreArchivo() : (opcion.getArchivoUrl() != null ? new java.io.File(opcion.getArchivoUrl()).getName() : null);
            this.archivoUrl = opcion.getArchivoUrl();
        } else if (opcion.getArchivoUrl() != null && !opcion.getArchivoUrl().isEmpty()) {
            this.archivoDisponible = true;
            java.io.File f = new java.io.File(opcion.getArchivoUrl());
            this.nombreArchivo = f.getName();
            this.archivoUrl = opcion.getArchivoUrl();
        } else {
            this.archivoDisponible = false;
        }
    }
}
