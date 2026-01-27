package com.rep.service.fx;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ThemeManager {

    private boolean darkMode = false;
    private final Set<Scene> activeScenes = new HashSet<>();
    private final String lightThemePath = "/Styles/main.css";
    private final String darkThemePath = "/Styles/dark.css";

    public void registerScene(Scene scene) {
        activeScenes.add(scene);
        applyTheme(scene);
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        activeScenes.forEach(this::applyTheme);
    }

    private void applyTheme(Scene scene) {
        Platform.runLater(() -> {
            ObservableList<String> stylesheets = scene.getStylesheets();

            // Resolve URLs safely
            String lightUrl = getResourceUrl(lightThemePath);
            String darkUrl = getResourceUrl(darkThemePath);

            // Remove existing themes if present
            if (lightUrl != null)
                stylesheets.remove(lightUrl);
            if (darkUrl != null)
                stylesheets.remove(darkUrl);

            // Also try removing by raw path just in case they were added that way
            // previously
            stylesheets.remove(lightThemePath);
            stylesheets.remove(darkThemePath);

            if (darkMode) {
                if (darkUrl != null)
                    stylesheets.add(darkUrl);
                else
                    System.err.println("Could not find dark theme: " + darkThemePath);
            } else {
                if (lightUrl != null)
                    stylesheets.add(lightUrl);
                else
                    System.err.println("Could not find light theme: " + lightThemePath);
            }
        });
    }

    private String getResourceUrl(String path) {
        java.net.URL url = getClass().getResource(path);
        return url != null ? url.toExternalForm() : null;
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
