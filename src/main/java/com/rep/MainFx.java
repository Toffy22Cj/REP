package com.rep;

import com.rep.service.fx.NavigationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MainFx extends Application {
    private ConfigurableApplicationContext context;
    private static Stage primaryStageHolder;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        context = new SpringApplicationBuilder(MainFx.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Guardar referencia del Stage principal
            primaryStageHolder = primaryStage;

            // Configurar el NavigationService
            NavigationService navigationService = context.getBean(NavigationService.class);
            navigationService.setPrimaryStage(primaryStage);

            // Configuración inicial del Stage
            primaryStage.setTitle("REP - Red de Estudiantes y Profesores");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.centerOnScreen();

            // Cargar vista inicial
            navigationService.navigateTo("/view/Login.fxml");

            primaryStage.show();
        } catch (Exception e) {
            showErrorAlert("Error al iniciar la aplicación: " + e.getMessage());
            Platform.exit();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStageHolder;
    }

    private void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}