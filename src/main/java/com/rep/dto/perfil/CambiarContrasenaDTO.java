package com.rep.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaDTO(
        @NotBlank(message = "Escriba su contraseña actual")
        String actual,
        @NotBlank(message = "Escriba la nueva contraseña")
        @Size(max = 100, message = "La contraseña no puede superar 100 caracteres")
        String nueva) {
}
