package com.rep.dto.actividad;

import com.rep.model.Opcion;
import lombok.Data;

@Data
public class OpcionResponse {
    private Long id;
    private String texto;
    private Boolean esCorrecta;
    // Constructor desde entidad
    public OpcionResponse(Opcion opcion) {
        this.id = opcion.getId();
        this.texto = opcion.getTexto();
        this.esCorrecta = opcion.getEsCorrecta();
    }
}
