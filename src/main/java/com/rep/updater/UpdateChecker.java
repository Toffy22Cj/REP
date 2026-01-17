package com.rep.updater;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);
    private Preferences prefs;
    private boolean updateAvailable = false;
    private String latestVersion = "";

    @PostConstruct
    public void init() {
        prefs = Preferences.userNodeForPackage(UpdateChecker.class);

        // Verificar si hay actualización pendiente por el flag file
        checkForUpdateFlag();

        // La verificación periódica se maneja mediante @Scheduled
        // Pero podemos forzar una al inicio si ha pasado mucho tiempo
        long lastCheck = prefs.getLong("last_update_check", 0);
        long currentTime = System.currentTimeMillis();
        long hoursSinceLastCheck = (currentTime - lastCheck) / (60 * 60 * 1000);

        if (hoursSinceLastCheck >= UpdateConfig.CHECK_INTERVAL_HOURS) {
            logger.info("Iniciando verificación de actualización por tiempo transcurrido...");
            scheduledUpdateCheck();
        }
    }

    private void checkForUpdateFlag() {
        try {
            File updateFlag = new File(UpdateConfig.UPDATE_FLAG_FILE);
            if (updateFlag.exists()) {
                String content = Files.readString(updateFlag.toPath());
                latestVersion = extractVersion(content);
                updateAvailable = true;

                logger.info("📢 Actualización pendiente detectada: {}", latestVersion);

                prefs.putBoolean("update_available", true);
                prefs.put("latest_version", latestVersion);
                prefs.put("last_notification", LocalDateTime.now().toString());
            }
        } catch (Exception e) {
            logger.error("Error verificando archivo flag de actualización: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = UpdateConfig.CHECK_INTERVAL_HOURS * 60 * 60 * 1000)
    public void scheduledUpdateCheck() {
        if (!UpdateConfig.AUTO_CHECK)
            return;

        new Thread(() -> {
            try {
                logger.info("🔍 Ejecutando verificación de actualizaciones automática (silent)...");

                // Intentar encontrar el JAR del AutoUpdater
                File updaterJar = new File("AutoUpdater.jar");
                if (updaterJar.exists()) {
                    ProcessBuilder pb = new ProcessBuilder(
                            "java", "-jar", "AutoUpdater.jar", "--silent");
                    pb.directory(new File("."));
                    pb.start();

                    prefs.putLong("last_update_check", System.currentTimeMillis());
                } else {
                    logger.warn("AutoUpdater.jar no encontrado. No se pudo realizar la verificación automática.");
                }

            } catch (Exception e) {
                logger.error("Error en proceso de verificación automática: {}", e.getMessage());
            }
        }, "UpdateCheckThread").start();
    }

    private String extractVersion(String content) {
        if (content.contains("versión")) {
            String[] parts = content.split("versión");
            if (parts.length > 1) {
                return parts[1].trim().split("\\s")[0];
            }
        }
        return "Desconocida";
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void markAsNotified() {
        updateAvailable = false;
        prefs.putBoolean("update_available", false);

        try {
            Files.deleteIfExists(Paths.get(UpdateConfig.UPDATE_FLAG_FILE));
            logger.info("Archivo flag de actualización eliminado tras notificación.");
        } catch (Exception e) {
            logger.error("No se pudo eliminar el archivo flag: {}", e.getMessage());
        }
    }

    public void launchFullUpdater() {
        try {
            File updaterJar = new File("AutoUpdater.jar");
            if (updaterJar.exists()) {
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", "AutoUpdater.jar");
                pb.start();
            } else {
                logger.error("No se pudo lanzar el actualizador: AutoUpdater.jar no existe.");
            }
        } catch (Exception e) {
            logger.error("Error lanzando actualizador: {}", e.getMessage());
        }
    }
}
