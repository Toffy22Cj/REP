package com.rep.dto.auth;

import com.rep.model.Usuario;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistroUsuarioDTO {
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    private String nombre;

    @NotBlank(message = "El apellido es requerido")
    @Size(min = 2, max = 50)
    private String apellido;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Correo inválido")
    private String correo;

    @NotBlank(message = "La identificación es requerida")
    @Size(min = 5, max = 20)
    private String identificacion;

    @NotBlank(message = "El tipo de identificación es requerido")
    private String tipoIdentificacion;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contraseña;

    @NotBlank(message = "El rol es requerido")
    private String rol;

    private boolean activo;
    private LocalDate fechaIngreso;
    private Usuario.Sexo sexo;

    @Min(5)
    @Max(120)
    private Integer edad;

    private Long cursoId;
}