package com.rep.dto.perfil;

import com.rep.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que cada usuario puede cambiar por sí mismo. Identificación, tipo de documento,
 * rol, edad y curso son datos institucionales: solo los cambia la administración.
 */
public record ActualizarPerfilDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        String apellido,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar 100 caracteres")
        String correo,
        Usuario.Sexo sexo) {
}
