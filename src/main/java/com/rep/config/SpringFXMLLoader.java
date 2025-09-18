package com.rep.config;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class SpringFXMLLoader {
    private final ApplicationContext context;

    @Autowired
    public SpringFXMLLoader(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Carga un archivo FXML desde el classpath
     * @param fxmlPath Ruta relativa del archivo FXML (ej: "/view/login.fxml")
     * @return Parent con la vista cargada
     * @throws IOException Si el archivo no se encuentra o hay errores de carga
     */
    public Parent load(String fxmlPath) throws IOException {
        // Verifica que la ruta comience con /
        if (!fxmlPath.startsWith("/")) {
            fxmlPath = "/" + fxmlPath;
        }

        // Obtiene la URL del recurso usando ClassPathResource
        URL fxmlUrl = new ClassPathResource(fxmlPath).getURL();

        if (fxmlUrl == null) {
            throw new IOException("Archivo FXML no encontrado: " + fxmlPath);
        }

        // Crea el FXMLLoader con la URL
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setControllerFactory(context::getBean);
        return loader.load();
    }

    /**
     * Versión alternativa que acepta URLs directamente
     * @param fxmlUrl URL completa del archivo FXML
     * @return Parent con la vista cargada
     * @throws IOException Si hay errores de carga
     */
    public Parent load(URL fxmlUrl) throws IOException {
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setControllerFactory(context::getBean);
        return loader.load();
    }
}