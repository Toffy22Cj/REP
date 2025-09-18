package com.rep.dto.auth;

public class LoginRequest {
    private String identificacion;
    private String password;

    // Constructor vacío
    public LoginRequest() {
    }

    // Constructor con parámetros
    public LoginRequest(String identificacion, String password) {
        this.identificacion = identificacion;
        this.password = password;
    }

    // Getters y setters
    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}