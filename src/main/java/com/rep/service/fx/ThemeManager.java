package com.rep.service.fx;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    // Use clear, absolute classpath references
    private static final String LIGHT_THEME = "/styles/main.css";
    private static final String DARK_THEME = "/styles/dark-mode.css";

    private boolean isDarkMode = false;
    private final List<Scene> registeredScenes = new ArrayList<>();

    public void registerScene(Scene scene) {
        if (scene == null)
            return;
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        applyTheme(scene);
    }

    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        registeredScenes.forEach(this::applyTheme);
        logger.info("Theme toggled. Dark mode: {}", isDarkMode);
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    private void applyTheme(Scene scene) {
        if (scene == null)
            return;

        // Remove old themes to avoid accumulation/conflict
        // Note: In a complex app you might want to remove specific sheets,
        // but clearing is safe if these are the only global styles.
        // For safer integration, we'll just remove known instances or add/remove
        // specifically.

        // Safer approach: Remove both potential themes first
        String lightForm = getResourceForm(LIGHT_THEME);
        String darkForm = getResourceForm(DARK_THEME);

        if (lightForm != null)
            scene.getStylesheets().remove(lightForm);
        if (darkForm != null)
            scene.getStylesheets().remove(darkForm);

        // Apply current theme
        String themePath = isDarkMode ? DARK_THEME : LIGHT_THEME;
        String cssUrl = getResourceForm(themePath);

        if (cssUrl != null) {
            if (!scene.getStylesheets().contains(cssUrl)) {
                scene.getStylesheets().add(cssUrl);
            }
        } else {
            logger.error("Could not find theme file: {}", themePath);
        }
    }

    /**
     * Helper to get the external form of the resource URL.
     * Uses getClass().getResource() which is JAR-safe.
     */
    private String getResourceForm(String path) {
        if (path == null)
            return null;

        // Ensure path DOES start with / for getClass().getResource()
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        URL url = getClass().getResource(path);
        if (url == null) {
            return null; // Let caller handle logging if needed
        }
        return url.toExternalForm();
    }
}
