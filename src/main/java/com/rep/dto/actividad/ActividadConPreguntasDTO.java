package com.rep.dto.actividad;


import lombok.Data;

import java.util.List;

@Data
public class ActividadConPreguntasDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private List<PreguntaConOpcionesDTO> preguntas;
}