package com.rep.dto.actividad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpcionRequest {
    @NotBlank(message = "El texto de la opción no puede estar vacío")
    @Size(max = 255, message = "El texto de la opción no puede exceder 255 caracteres")
    private String texto;

    @NotNull(message = "El campo 'esCorrecta' es requerido")
    private Boolean esCorrecta;

}
