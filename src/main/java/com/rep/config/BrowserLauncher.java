package com.rep.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Lanza el navegador automáticamente al iniciar el servidor
 */
@Component
@Slf4j
public class BrowserLauncher {

    @Value("${server.port:8080}")
    private String port;

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        String url = "http://localhost:" + port + "/admin/";

        // Evitar lanzar en entornos headless (servidores remotos reales)
        if (System.getProperty("java.awt.headless") != null && System.getProperty("java.awt.headless").equals("true")) {
            log.info("Entorno Headless detectado. No se lanzará el navegador. URL del panel: {}", url);
            return;
        }

        log.info("Lanzando navegador hacia: {}", url);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                Runtime rt = Runtime.getRuntime();
                if (os.contains("win")) {
                    rt.exec("round /c start " + url);
                } else if (os.contains("mac")) {
                    rt.exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    rt.exec("xdg-open " + url);
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.error("No se pudo abrir el navegador automáticamente: {}", e.getMessage());
        }
    }
}
