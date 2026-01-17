package com.rep.controller.views;

import com.rep.updater.GitHubUpdateService;
import com.rep.updater.UpdateChecker;
import com.rep.updater.UpdateConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.nio.file.Files;

@Controller
public class UpdatePanelController {

    private static final Logger logger = LoggerFactory.getLogger(UpdatePanelController.class);

    @Autowired
    private UpdateChecker updateChecker;

    @FXML
    private Label currentVersionLabel;
    @FXML
    private Label latestVersionLabel;
    @FXML
    private TextArea releaseNotesArea;
    @FXML
    private Label updateStatusLabel;
    @FXML
    private ProgressIndicator updateProgress;
    @FXML
    private Button installBtn;
    @FXML
    private Button checkBtn;

    @FXML
    public void initialize() {
        String currentVersion = getCurrentVersionLocally();
        currentVersionLabel.setText(currentVersion);

        if (updateChecker.isUpdateAvailable()) {
            latestVersionLabel.setText(updateChecker.getLatestVersion());
            updateStatusLabel.setText("🔄 Actualización disponible");
            updateStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            installBtn.setDisable(false);
        } else {
            latestVersionLabel.setText("No verificado");
            updateStatusLabel.setText("✅ Busque actualizaciones");
            updateStatusLabel.setStyle("-fx-text-fill: #7f8c8d;");
        }
    }

    @FXML
    public void onCheckForUpdates() {
        checkBtn.setDisable(true);
        updateProgress.setVisible(true);
        updateStatusLabel.setText("Conectando con GitHub...");

        new Thread(() -> {
            try {
                GitHubUpdateService.ReleaseInfo latest = GitHubUpdateService.getLatestRelease();
                String current = getCurrentVersionLocally();

                Platform.runLater(() -> {
                    latestVersionLabel.setText(latest.getVersion());
                    releaseNotesArea.setText(latest.getReleaseNotes());

                    if (!latest.getVersion().equals(current)) {
                        updateStatusLabel.setText("¡Nueva versión disponible!");
                        updateStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        installBtn.setDisable(false);
                    } else {
                        updateStatusLabel.setText("✅ Tienes la última versión.");
                        updateStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
                        installBtn.setDisable(true);
                    }
                    updateProgress.setVisible(false);
                    checkBtn.setDisable(false);
                });
            } catch (Exception e) {
                logger.error("Error al buscar actualizaciones manuales", e);
                Platform.runLater(() -> {
                    updateStatusLabel.setText("❌ Error: " + e.getMessage());
                    updateStatusLabel.setStyle("-fx-text-fill: #c0392b;");
                    updateProgress.setVisible(false);
                    checkBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    public void onInstallUpdate() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar actualización");
        confirm.setHeaderText("¿Desea iniciar el actualizador?");
        confirm.setContentText("Se abrirá una ventana independiente para gestionar la instalación.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            updateChecker.launchFullUpdater();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Actualizador iniciado");
            info.setHeaderText(null);
            info.setContentText(
                    "El actualizador se está ejecutando. Puede cerrar esta aplicación si el actualizador lo solicita.");
            info.showAndWait();
        }
    }

    private String getCurrentVersionLocally() {
        try {
            File versionFile = new File(UpdateConfig.VERSION_FILE);
            if (versionFile.exists()) {
                return Files.readString(versionFile.toPath()).trim();
            }
        } catch (Exception e) {
            logger.warn("No se pudo leer la versión local para la UI");
        }
        return "1.0.0";
    }
}
