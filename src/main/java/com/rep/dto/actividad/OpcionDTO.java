package com.rep.dto.actividad;

import lombok.Data;

@Data
public class OpcionDTO {
    private Long id;
    private String texto;
    private Boolean esCorrecta;
}