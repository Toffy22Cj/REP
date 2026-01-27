package com.rep.service.fx;

import com.rep.MainFx;
import com.rep.config.SpringFXMLLoader;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NavigationService {

    private final SpringFXMLLoader fxmlLoader;
    private final ThemeManager themeManager;
    private Stage primaryStage;
    private StackPane contentArea;
    private Label lblPageTitle;

    // Fixed sizes for consistency
    public static final double DEFAULT_WIDTH = 1100;
    public static final double DEFAULT_HEIGHT = 750;

    @Autowired
    public NavigationService(SpringFXMLLoader fxmlLoader, ThemeManager themeManager) {
        this.fxmlLoader = fxmlLoader;
        this.themeManager = themeManager;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initial window configuration
        primaryStage.setWidth(DEFAULT_WIDTH);
        primaryStage.setHeight(DEFAULT_HEIGHT);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.centerOnScreen();
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void setPageTitleLabel(Label lblPageTitle) {
        this.lblPageTitle = lblPageTitle;
    }

    public void navigateTo(String fxmlPath) {
        if (Platform.isFxApplicationThread()) {
            performNavigation(fxmlPath);
        } else {
            Platform.runLater(() -> performNavigation(fxmlPath));
        }
    }

    private void performNavigation(String fxmlPath) {
        try {
            // Reset state for new view before loading
            contentArea = null;
            lblPageTitle = null;

            Stage stage = (primaryStage != null) ? primaryStage : MainFx.getPrimaryStage();

            // ... (rest of the method stays mostly same, just removing the trailing resets)

            if (stage == null) {
                // Try one last desperate attempt to find a stage from active windows
                // This is a last resort fallback if both injections failed
                /*
                 * stage = Stage.getWindows().stream()
                 * .filter(Window::isShowing)
                 * .filter(w -> w instanceof Stage)
                 * .map(w -> (Stage) w)
                 * .findFirst()
                 * .orElse(null);
                 */
                // Logic to handle uninitialized stage usually implies configuration error
                System.err
                        .println("CRITICAL ERROR: Primary Stage not initialized. Navigation rejected for: " + fxmlPath);
                return;
                // We return instead of throwing to avoid crashing the thread loop if this is a
                // recurring event
            }

            Parent view = fxmlLoader.load(fxmlPath);
            // ... rest of logic

            // Disable fade for debugging
            view.setOpacity(1.0);

            // FadeTransition fade = new FadeTransition(Duration.millis(300), view);
            // fade.setFromValue(0);
            // fade.setToValue(1);

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(view);
                stage.setScene(scene);
                try {
                    themeManager.registerScene(scene);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to register scene with ThemeManager: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                scene.setRoot(view);
            }

            // fade.play();
            // fade.setOnFinished(e -> view.setOpacity(1));

            stage.setTitle("REP - Sistema Escolar");

            if (!stage.isShowing()) {
                stage.show();
            }

            System.out.println("DEBUG: Navigation to " + fxmlPath + " completed successfully. View visible: "
                    + view.isVisible() + ", Opacity: " + view.getOpacity());

        } catch (IOException e) {
            showErrorAlert("Error al cargar la vista root: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void navigateToContent(String fxmlPath, String title) {
        if (contentArea == null) {
            // If we are not in BaseLayout, first navigate to BaseLayout
            navigateTo("/view/BaseLayout.fxml");
            // We need a way to delay the content load until BaseLayout is initialized
            Platform.runLater(() -> navigateToContent(fxmlPath, title));
            return;
        }

        try {
            Parent view = fxmlLoader.load(fxmlPath);

            if (lblPageTitle != null) {
                lblPageTitle.setText(title);
            }

            // Smooth transition for internal content
            view.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(250), view);
            fade.setFromValue(0);
            fade.setToValue(1);

            contentArea.getChildren().setAll(view);
            fade.play();

        } catch (IOException e) {
            showErrorAlert("Error al cargar contenido interno: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void applyStylesheets(Scene scene, String fxmlPath) {
        // This is now managed globally by ThemeManager, but kept for compatibility
        themeManager.registerScene(scene);
    }

    private void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Navegación");
            alert.setHeaderText("No se pudo cargar la vista");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}