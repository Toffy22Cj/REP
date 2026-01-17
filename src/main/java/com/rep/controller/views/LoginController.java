package com.rep.controller.views;

import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.dto.auth.LoginResponse;
import com.rep.model.Usuario;
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
    private Button btnLogin;
    @FXML
    private ProgressIndicator progressIndicator;

    private final NavigationService navigationService;
    private final AuthServiceClient authServiceClient;
    private final JwtTokenHolder jwtTokenHolder;

    @Autowired
    public LoginController(NavigationService navigationService,
            AuthServiceClient authServiceClient,
            JwtTokenHolder jwtTokenHolder) {
        this.navigationService = navigationService;
        this.authServiceClient = authServiceClient;
        this.jwtTokenHolder = jwtTokenHolder;
    }

    @FXML
    public void initialize() {
        resetView();
    }

    private void resetView() {
        if (txtIdentificacion != null)
            txtIdentificacion.clear();
        if (txtPassword != null)
            txtPassword.clear();
        if (lblMensaje != null)
            lblMensaje.setText("");
        if (progressIndicator != null)
            progressIndicator.setVisible(false);
        if (btnLogin != null)
            btnLogin.setDisable(false);
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
                        cargarVistaAdmin(event);
                        break;
                    case PROFESOR:
                        cargarVistaProfesor(event);
                        break;
                    case ESTUDIANTE:
                        cargarVistaEstudiante(event);
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

    private void cargarVistaAdmin(ActionEvent event) {
        navigationService.navigateTo("/view/VistaAdmin.fxml");
    }

    private void cargarVistaProfesor(ActionEvent event) {
        navigationService.navigateTo("/view/VistaMaestro.fxml");
    }

    private void cargarVistaEstudiante(ActionEvent event) {
        navigationService.navigateTo("/view/VistaEstudiante.fxml");
    }

    @FXML
    private void registro() {
        navigationService.navigateTo("/view/Registro.fxml");
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: red;");
    }
}