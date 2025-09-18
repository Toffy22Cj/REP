package com.rep.controller.views;

import com.rep.dto.tokens.JwtTokenHolder;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTokenController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected JwtTokenHolder jwtTokenHolder;

    public void setJwtTokenHolder(JwtTokenHolder jwtTokenHolder) {
        this.jwtTokenHolder = jwtTokenHolder;
    }

    protected String obtenerToken() {
        try {
            if (jwtTokenHolder == null) {
                throw new IllegalStateException("JwtTokenHolder no ha sido inicializado");
            }

            String token = jwtTokenHolder.getToken();
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Token JWT no está disponible o está vacío");
            }

            return token.startsWith("Bearer ") ? token.substring(7) : token;
        } catch (Exception e) {
            logger.error("Error al obtener token: {}", e.getMessage());
            mostrarEstado("Error: Autenticación requerida", Color.RED, getEstadoLabel());
            throw new SecurityException("No se pudo validar el token de acceso", e);
        }
    }

    protected abstract Label getEstadoLabel();

    protected void mostrarEstado(String mensaje, Color color, Label label) {
        if (label != null) {
            label.setText(mensaje);
            label.setTextFill(color);
        }
    }

    protected void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}