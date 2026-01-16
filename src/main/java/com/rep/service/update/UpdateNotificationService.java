package com.rep.service.update;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar verificación y notificación de actualizaciones.
 * 
 * Funcionalidades:
 * - Verificación periódica de nuevas versiones en GitHub
 * - Notificación al usuario cuando hay actualizaciones disponibles
 * - Gestión de preferencias de actualización
 * - Descarga automática opcional
 * 
 * @author Sistema Educativo REP
 * @version 1.0.0
 */
@Service
public class UpdateNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(UpdateNotificationService.class);

    // Configuración
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Toffy22Cj/REP/releases/latest";
    private static final String VERSION_FILE = "version.json";
    private static final long CHECK_INTERVAL_DAYS = 7;
    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);

    @org.springframework.beans.factory.annotation.Value("${app.update-check.enabled:true}")
    private boolean enabled;

    // Estado
    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = "";
    private volatile String releaseNotes = "";
    private volatile String downloadUrl = "";
    private volatile long lastCheckTime = 0;

    @PostConstruct
    public void init() {
        if (!enabled) {
            logger.info("El servicio de actualizaciones está deshabilitado");
            return;
        }

        // Cargar última vez que se verificó
        loadUpdateInfo();

        // Verificar si es necesario chequear actualizaciones
        if (shouldCheckForUpdates()) {
            logger.info("Verificando actualizaciones en segundo plano...");
            checkForUpdatesInBackground();
        } else {
            long lastPlusInterval = lastCheckTime + (CHECK_INTERVAL_DAYS * ONE_DAY_MS);
            long daysUntilNextCheck = Math.max(0, (lastPlusInterval - System.currentTimeMillis()) / ONE_DAY_MS);
            logger.info("Próxima verificación de actualizaciones en {} días", daysUntilNextCheck);
        }
    }

    /**
     * Tarea programada para verificar actualizaciones cada 24 horas.
     */
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000, initialDelay = 60 * 1000)
    public void scheduledUpdateCheck() {
        if (enabled && shouldCheckForUpdates()) {
            logger.debug("Ejecutando verificación programada de actualizaciones");
            checkForUpdatesInBackground();
        }
    }

    /**
     * Verifica si es necesario chequear actualizaciones.
     */
    private boolean shouldCheckForUpdates() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCheck = currentTime - lastCheckTime;
        return timeSinceLastCheck > (CHECK_INTERVAL_DAYS * ONE_DAY_MS);
    }

    /**
     * Verifica actualizaciones en un hilo separado.
     */
    public void checkForUpdatesInBackground() {
        new Thread(() -> {
            try {
                logger.info("Conectando con GitHub para verificar actualizaciones...");

                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "SistemaEducativo-REP");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parsear respuesta JSON manualmente (sin dependencias externas)
                    String json = response.toString();
                    parseReleaseInfo(json);

                    // Actualizar timestamp
                    lastCheckTime = System.currentTimeMillis();
                    saveUpdateInfo();

                    if (updateAvailable) {
                        logger.info("¡Nueva versión disponible! v{}", latestVersion);
                        notifyUpdateAvailable();
                    } else {
                        logger.info("Sistema actualizado. Versión actual: {}", getCurrentVersion());
                    }

                } else if (responseCode == 403) {
                    logger.warn("Límite de API de GitHub alcanzado. Reintentando más tarde.");
                } else {
                    logger.warn("Error al verificar actualizaciones: HTTP {}", responseCode);
                }

            } catch (Exception e) {
                logger.error("Error al verificar actualizaciones: {}", e.getMessage(), e);
            }
        }, "UpdateChecker").start();
    }

    /**
     * Parsea la información del release desde el JSON de GitHub.
     */
    private void parseReleaseInfo(String json) {
        try {
            // Extraer tag_name
            latestVersion = extractJsonValue(json, "\"tag_name\":\\s*\"([^\"]+)\"");
            if (latestVersion.startsWith("v")) {
                latestVersion = latestVersion.substring(1);
            }

            // Extraer body (release notes)
            releaseNotes = extractJsonValue(json, "\"body\":\\s*\"([^\"]+)\"");
            releaseNotes = releaseNotes.replace("\\n", "\n").replace("\\r", "");

            // Extraer download URL del primer asset
            String assetsSection = extractJsonValue(json, "\"assets\":\\s*\\[([^\\]]+)\\]");
            if (!assetsSection.isEmpty()) {
                downloadUrl = extractJsonValue(assetsSection,
                        "\"browser_download_url\":\\s*\"([^\"]+)\"");
            }

            // Comparar versiones
            String currentVersion = getCurrentVersion();
            updateAvailable = isNewerVersion(latestVersion, currentVersion);

            logger.debug("Versión actual: {}", currentVersion);
            logger.debug("Última versión: {}", latestVersion);
            logger.debug("Actualización disponible: {}", updateAvailable);

        } catch (Exception e) {
            logger.error("Error al parsear información de release", e);
        }
    }

    /**
     * Extrae un valor de un JSON usando regex simple.
     */
    private String extractJsonValue(String json, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Obtiene la versión actual del archivo version.json.
     */
    private String getCurrentVersion() {
        try {
            // Intentar leer desde el classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream(VERSION_FILE);
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();

                String json = content.toString();
                String version = extractJsonValue(json, "\"version\":\\s*\"([^\"]+)\"");
                return version.isEmpty() ? "1.0.0" : version;
            }
        } catch (Exception e) {
            logger.warn("No se pudo leer version.json: {}", e.getMessage());
        }
        return "1.0.0";
    }

    /**
     * Compara dos versiones semánticas (ej: 1.2.3).
     * 
     * @return true si latest es más nueva que current
     */
    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");

            int maxLength = Math.max(latestParts.length, currentParts.length);

            for (int i = 0; i < maxLength; i++) {
                int latestNum = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentNum = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

                if (latestNum > currentNum)
                    return true;
                if (latestNum < currentNum)
                    return false;
            }

        } catch (NumberFormatException e) {
            logger.warn("Error al comparar versiones: {} vs {}", latest, current);
        }

        return false;
    }

    /**
     * Carga información de actualizaciones desde archivo local.
     */
    private void loadUpdateInfo() {
        try {
            Path updateFile = Paths.get("update.info");
            if (Files.exists(updateFile)) {
                String content = Files.readString(updateFile);
                String timestampStr = extractJsonValue(content,
                        "\"last_check\":\\s*(\\d+)");
                if (!timestampStr.isEmpty()) {
                    lastCheckTime = Long.parseLong(timestampStr);
                }
            }
        } catch (Exception e) {
            logger.debug("No se pudo cargar update.info: {}", e.getMessage());
        }
    }

    /**
     * Guarda información de la última verificación.
     */
    private void saveUpdateInfo() {
        try {
            String json = String.format(
                    "{\"last_check\":%d,\"update_available\":%b,\"latest_version\":\"%s\"}",
                    lastCheckTime, updateAvailable, latestVersion);
            Files.writeString(Paths.get("update.info"), json);
        } catch (Exception e) {
            logger.warn("No se pudo guardar update.info: {}", e.getMessage());
        }
    }

    /**
     * Notifica al usuario que hay una actualización disponible.
     */
    private void notifyUpdateAvailable() {
        try {
            // Crear archivo de notificación para la UI
            String notification = String.format(
                    "Nueva versión %s disponible%n%n%s%n%nDescargar: %s",
                    latestVersion,
                    releaseNotes.substring(0, Math.min(500, releaseNotes.length())),
                    downloadUrl);

            Files.writeString(Paths.get("update.available"), notification);
            logger.info("Archivo de notificación creado: update.available");

        } catch (Exception e) {
            logger.warn("No se pudo crear archivo de notificación", e);
        }
    }

    // Métodos públicos para la UI

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void markAsNotified() {
        try {
            Files.deleteIfExists(Paths.get("update.available"));
        } catch (Exception e) {
            logger.warn("No se pudo eliminar notificación", e);
        }
    }

    /**
     * Fuerza una verificación inmediata de actualizaciones.
     */
    public void checkNow() {
        logger.info("Verificación manual de actualizaciones iniciada");
        checkForUpdatesInBackground();
    }
}
