package com.rep.controller.views;

import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.dto.auth.LoginResponse;
import com.rep.service.funciones.AuthServiceClient;
import com.rep.service.fx.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class LoginController {
    @FXML
    private TextField txtIdentificacion;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMensaje;
    @FXML
    private Button btnRegistro;
    @FXML
    private Button btnLogin;
    @FXML
    private ProgressIndicator progressIndicator;

    private final NavigationService navigationService;
    private final AuthServiceClient authServiceClient;

    @Autowired
    private JwtTokenHolder jwtTokenHolder;

    @Autowired
    public LoginController(NavigationService navigationService,
            AuthServiceClient authServiceClient) {
        this.navigationService = navigationService;
        this.authServiceClient = authServiceClient;
    }

    @FXML
    public void initialize() {
        resetView();
    }

    private void resetView() {
        txtIdentificacion.clear();
        txtPassword.clear();
        lblMensaje.setText("");
        progressIndicator.setVisible(false);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String identificacion = txtIdentificacion.getText().trim();
        String password = txtPassword.getText().trim();

        if (identificacion.isEmpty() || password.isEmpty()) {
            mostrarError("Ingrese identificación y contraseña");
            return;
        }

        try {
            progressIndicator.setVisible(true);
            btnLogin.setDisable(true);

            Optional<LoginResponse> response = authServiceClient.authenticate(identificacion, password);

            if (response.isPresent() && response.get().isSuccess()) {
                LoginResponse loginResponse = response.get();
                jwtTokenHolder.setToken(loginResponse.getToken());

                switch (loginResponse.getUsuario().getRol()) {
                    case ADMIN:
                        navigationService.navigateToContent("/view/VistaAdmin.fxml", "Panel de Administración");
                        break;
                    case PROFESOR:
                        navigationService.navigateToContent("/view/VistaMaestro.fxml", "Panel de Docente");
                        break;
                    case ESTUDIANTE:
                        navigationService.navigateToContent("/view/VistaEstudiante.fxml", "Área de Estudiante");
                        break;
                }
            } else {
                mostrarError(response.map(LoginResponse::getMensaje)
                        .orElse("Error en la autenticación"));
            }
        } catch (Exception e) {
            mostrarError("Error al conectar con el servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            progressIndicator.setVisible(false);
            btnLogin.setDisable(false);
        }
    }

    @FXML
    private void registro() {
        navigationService.navigateTo("/view/Registro.fxml");
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: green;");
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: red;");
    }
}