package com.rep.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Actualización parcial de un usuario: los campos nulos no se modifican. Antes el
 * endpoint sobrescribía todo (p. ej. dejaba el apellido en null si el cliente no lo
 * enviaba, lo que provocaba un error 500 al guardar).
 */
@Data
public class ActualizarUsuarioDTO {
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String apellido;

    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    /** Vacío o nulo = no cambiar la contraseña. */
    @Size(max = 100, message = "La contraseña no puede superar 100 caracteres")
    private String contraseña;

    @Pattern(regexp = "ADMIN|PROFESOR|ESTUDIANTE", message = "El rol debe ser ADMIN, PROFESOR o ESTUDIANTE")
    private String rol;

    private Boolean activo;
}
