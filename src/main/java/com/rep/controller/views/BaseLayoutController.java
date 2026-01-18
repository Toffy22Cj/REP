package com.rep.controller.views;

import com.rep.service.fx.NavigationService;
import com.rep.service.fx.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class BaseLayoutController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label lblPageTitle;
    @FXML
    private Label lblUserName;
    @FXML
    private Label lblUserRole;
    @FXML
    private Circle userAvatar;

    private final NavigationService navigationService;
    private final ThemeManager themeManager;

    @Autowired
    public BaseLayoutController(NavigationService navigationService, ThemeManager themeManager) {
        this.navigationService = navigationService;
        this.themeManager = themeManager;
    }

    @FXML
    public void initialize() {
        // Shared logic for the layout
        navigationService.setContentArea(contentArea);
        navigationService.setPageTitleLabel(lblPageTitle);

        // Mock user info (should be replaced with actual session data)
        lblUserName.setText("Panel de Control");
        lblUserRole.setText("Sistema Educativo");
    }

    @FXML
    public void onDashboard() {
        navigationService.navigateToContent("/view/ResultadosActividad.fxml", "Dashboard");
    }

    @FXML
    public void onCourses() {
        // Example courses view
        navigationService.navigateToContent("/view/VistaEstudiante.fxml", "Mis Cursos");
    }

    @FXML
    public void onAttendance() {
        navigationService.navigateToContent("/view/asistencia.fxml", "Asistencia");
    }

    @FXML
    public void onReports() {
        navigationService.navigateToContent("/view/VistaAdmin.fxml", "Reportes Globales");
    }

    @FXML
    public void onSettings() {
        navigationService.navigateToContent("/view/editor_preguntas.fxml", "Configuración");
    }

    @FXML
    public void toggleTheme() {
        themeManager.toggleTheme();
    }

    @FXML
    public void handleLogout() {
        navigationService.navigateTo("/view/Login.fxml");
    }
}
