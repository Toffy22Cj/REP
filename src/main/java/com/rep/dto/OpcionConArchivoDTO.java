package com.rep.dto;

import lombok.Data;

@Data
public class OpcionConArchivoDTO {
    private String texto;
    private Boolean esCorrecta;
    private boolean tieneArchivo;
    private String nombreArchivo;
    private String archivoTipo;
    private byte[] archivoData;
    private String archivoBase64;
}
