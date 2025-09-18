package com.rep.dto.actividad;

import com.rep.model.Materia;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateriaDTO {
    private Long id;
    private String nombre;

    public MateriaDTO(Materia materia) {
        this.id = materia.getId();
        this.nombre = materia.getNombre();
    }
}