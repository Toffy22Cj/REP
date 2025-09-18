package com.rep.dto.auth;

import lombok.Data;

@Data
public class LoginResponse {
    private AuthUsuarioDTO usuario;
    private String token;
    private String mensaje;
    private boolean success;

    // Constructor con 4 parámetros
    public LoginResponse(AuthUsuarioDTO usuario, String token, String mensaje, boolean success) {
        this.usuario = usuario;
        this.token = token;
        this.mensaje = mensaje;
        this.success = success;
    }

    // Método estático para crear respuesta de éxito
    public static LoginResponse success(AuthUsuarioDTO usuario, String token, String mensaje) {
        return new LoginResponse(usuario, token, mensaje, true);
    }

    // Método estático para crear respuesta de error
    public static LoginResponse error(String mensaje) {
        return new LoginResponse(null, null, mensaje, false);
    }
}