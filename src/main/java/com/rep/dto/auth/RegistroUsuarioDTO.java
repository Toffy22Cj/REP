package com.rep.dto.auth;

import com.rep.model.Usuario;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/** Datos de alta de un usuario (auto-registro y administración). */
@Data
public class RegistroUsuarioDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String apellido;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 100, message = "El correo no puede superar 100 caracteres")
    private String correo;

    @NotBlank(message = "La identificación es obligatoria")
    @Pattern(regexp = "\\d{5,20}", message = "La identificación debe tener entre 5 y 20 dígitos (solo números)")
    private String identificacion;

    @NotBlank(message = "El tipo de identificación es obligatorio")
    @Pattern(regexp = "CC|TI", message = "El tipo de identificación debe ser CC o TI")
    private String tipoIdentificacion;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    private String contraseña;

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "ADMIN|PROFESOR|ESTUDIANTE", message = "El rol debe ser ADMIN, PROFESOR o ESTUDIANTE")
    private String rol;

    private boolean activo;
    private LocalDate fechaIngreso;
    private Usuario.Sexo sexo;

    @Min(value = 5, message = "La edad mínima es 5 años")
    @Max(value = 120, message = "La edad máxima es 120 años")
    private Integer edad;

    private Long cursoId;
}
