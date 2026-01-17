package com.rep.service.fx;

import com.rep.MainFx;
import com.rep.config.SpringFXMLLoader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, String> STYLESHEET_MAP = new HashMap<>();
    private static final Map<String, double[]> VIEW_SIZES = Map.of(
            "/view/Login.fxml", new double[] { 800, 600 },
            "/view/Registro.fxml", new double[] { 800, 600 },
            "/view/VistaAdmin.fxml", new double[] { 1200, 800 },
            "/view/VistaMaestro.fxml", new double[] { 1100, 750 },
            "/view/VistaEstudiante.fxml", new double[] { 1000, 700 },
            "/view/editor_preguntas.fxml", new double[] { 1200, 850 },
            "/view/asistencia.fxml", new double[] { 900, 650 });

    @Autowired
    public NavigationService(SpringFXMLLoader fxmlLoader) {
        this.fxmlLoader = fxmlLoader;
        initializeStylesheetMap();
    }

    private void initializeStylesheetMap() {
        STYLESHEET_MAP.put("/view/Login.fxml", "/styles/login.css");
        STYLESHEET_MAP.put("/view/Registro.fxml", "/styles/login.css");
        STYLESHEET_MAP.put("/view/VistaAdmin.fxml", "/styles/admin.css");
        STYLESHEET_MAP.put("/view/VistaMaestro.fxml", "/styles/maestro.css");
        STYLESHEET_MAP.put("/view/VistaEstudiante.fxml", "/styles/estudiante.css");
        STYLESHEET_MAP.put("/view/editor_preguntas.fxml", "/styles/preguntas.css");
        STYLESHEET_MAP.put("/view/asistencia.fxml", "/styles/maestro.css");
        STYLESHEET_MAP.put("/view/ResultadosActividad.fxml", "/styles/maestro.css");
        STYLESHEET_MAP.put("/view/DetalleRespuesta.fxml", "/styles/maestro.css");
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void navigateTo(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                Stage stage = (primaryStage != null) ? primaryStage : MainFx.getPrimaryStage();

                if (stage == null) {
                    throw new RuntimeException("No hay Stage principal disponible para navegar");
                }

                Parent view = fxmlLoader.load(fxmlPath);
                Scene scene = stage.getScene();

                if (scene == null) {
                    scene = new Scene(view);
                    stage.setScene(scene);
                } else {
                    scene.setRoot(view);
                }

                // Ajustar tamaño del Stage según la vista
                double[] size = VIEW_SIZES.get(fxmlPath);
                if (size != null) {
                    stage.setWidth(size[0]);
                    stage.setHeight(size[1]);
                }

                if (!stage.isShowing()) {
                    stage.show();
                }

                // Centrar la ventana
                stage.centerOnScreen();

                // Cargar CSS programáticamente para asegurar compatibilidad con JAR
                applyStylesheets(scene, fxmlPath);

                System.out.println("🚀 Navegación exitosa a: " + fxmlPath);

            } catch (IOException e) {
                showErrorAlert("Error al cargar la vista: " + e.getMessage());
                throw new RuntimeException("Error navegando a: " + fxmlPath, e);
            }
        });
    }

    public void applyStylesheets(Scene scene, String fxmlPath) {
        if (scene == null)
            return;

        String cssPath = STYLESHEET_MAP.get(fxmlPath);
        if (cssPath != null) {
            try {
                URL cssUrl = getClass().getResource(cssPath);
                if (cssUrl != null) {
                    scene.getStylesheets().clear();
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    // También agregar CSS base si existe
                    URL baseCss = getClass().getResource("/styles/base.css");
                    if (baseCss != null) {
                        scene.getStylesheets().add(baseCss.toExternalForm());
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error al aplicar CSS: " + e.getMessage());
            }
        }
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