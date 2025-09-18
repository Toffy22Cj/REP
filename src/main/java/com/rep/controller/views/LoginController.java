package com.rep.controller.views;

import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.dto.auth.LoginResponse;
import com.rep.service.funciones.AdminApiService;
import com.rep.service.funciones.AuthServiceClient;
import com.rep.service.fx.NavigationService;
import com.rep.service.logica.UsuarioRegistrationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Optional;

@Controller
public class LoginController {
    @FXML private TextField txtIdentificacion;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;
    @FXML private Button btnRegistro;
    @FXML private Button btnLogin;
    @FXML private ProgressIndicator progressIndicator;

    private final NavigationService navigationService;
    private final UsuarioRegistrationService registrationService;
    private final AuthServiceClient authServiceClient;

    @Autowired
    private JwtTokenHolder jwtTokenHolder;
    @Autowired
    AdminApiService adminApiService;
    public LoginController(NavigationService navigationService,
                           UsuarioRegistrationService registrationService,
                           AuthServiceClient authServiceClient ,AdminApiService adminApiService) {
        this.adminApiService = adminApiService;
        this.navigationService = navigationService;
        this.registrationService = registrationService;
        this.authServiceClient = authServiceClient;
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

                switch(loginResponse.getUsuario().getRol()) {
                    case ADMIN:
                        cargarVistaAdmin(event);
                        break;
                    case PROFESOR:
                        navigationService.navigateTo("/view/VistaMaestro.fxml");
                        break;
                    case ESTUDIANTE:
                        navigationService.navigateTo("/view/VistaEstudiante.fxml");
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/VistaAdmin.fxml"));
            loader.setControllerFactory(param -> {
                try {
                    return new AdminPanelController(adminApiService, registrationService, jwtTokenHolder);
                } catch (Exception e) {
                    throw new RuntimeException("Error al crear AdminPanelController", e);
                }
            });

            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            ((Node) event.getSource()).getScene().getWindow().hide();
        } catch (IOException e) {
            mostrarError("Error al cargar la vista de administrador");
            e.printStackTrace();
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