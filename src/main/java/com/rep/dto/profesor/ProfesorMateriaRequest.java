package com.rep.dto.profesor;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfesorMateriaRequest {
    @NotNull
    private Long profesorId;

    @NotNull
    private Long materiaId;

    @NotNull
    private Long cursoId;

}
