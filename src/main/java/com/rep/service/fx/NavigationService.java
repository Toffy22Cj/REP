package com.rep.service.fx;

import com.rep.MainFx;
import com.rep.config.SpringFXMLLoader;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NavigationService {

    private final SpringFXMLLoader fxmlLoader;
    private Stage primaryStage;

    @Autowired
    public NavigationService(SpringFXMLLoader fxmlLoader) {
        this.fxmlLoader = fxmlLoader;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    public void navigateTo(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                Stage stage = (primaryStage != null) ? primaryStage : MainFx.getPrimaryStage();

                if (stage == null) {
                    // Si no hay stage disponible, crear uno nuevo
                    stage = new Stage();
                    primaryStage = stage;
                }

                Parent view = fxmlLoader.load(fxmlPath);
                Scene scene = stage.getScene();

                if (scene == null) {
                    scene = new Scene(view);
                    stage.setScene(scene);
                } else {
                    scene.setRoot(view);
                }

                if (!stage.isShowing()) {
                    stage.show();
                }
            } catch (IOException e) {
                showErrorAlert("Error al cargar la vista: " + e.getMessage());
                throw new RuntimeException("Error navegando a: " + fxmlPath, e);
            }
        });
    }

    private void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}