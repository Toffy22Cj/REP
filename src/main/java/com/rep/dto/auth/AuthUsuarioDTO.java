package com.rep.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rep.model.Usuario;
import com.rep.model.Usuario.Rol;
import lombok.Data;

@Data
public class AuthUsuarioDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String correo;
    private String identificacion;
    private Rol rol;

    @JsonCreator
    public AuthUsuarioDTO(
            @JsonProperty("id") Long id,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("apellido") String apellido,
            @JsonProperty("correo") String correo,
            @JsonProperty("identificacion") String identificacion,
            @JsonProperty("rol") Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.identificacion = identificacion;
        this.rol = rol;
    }

    public AuthUsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.correo = usuario.getCorreo();
        this.identificacion = usuario.getIdentificacion();
        this.rol = usuario.getRol();
    }
}