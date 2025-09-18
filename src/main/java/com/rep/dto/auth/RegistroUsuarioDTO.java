package com.rep.dto.auth;

import com.rep.model.Usuario;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
@Data
public class RegistroUsuarioDTO {
    private String nombre;
    private String correo;
    private String identificacion;
    private String tipoIdentificacion;
    private String contraseña;
    private String rol;
    private boolean activo;
    private LocalDate fechaIngreso;
    private Usuario.Sexo sexo;
    // En RegistroUsuarioDTO.java
// Quita @NotNull y hazlo condicional en el servicio
    @Min(5) @Max(120)
    private Integer edad;
    private Long cursoId;
}