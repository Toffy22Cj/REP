package com.rep.dto.actividad;

import lombok.Data;

@Data
public class RespuestaPreguntaDetalleDTO {
    private Long id;
    private Long preguntaId;
    private String enunciado;
    private Long opcionId;
    private String opcionTexto;
    private String respuestaAbierta;
    private String archivoAdjunto;
    private String nombreArchivo;
    private Boolean esCorrecta;
}
