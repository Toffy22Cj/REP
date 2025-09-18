package com.rep;

import com.rep.config.SpringFXMLLoader;
import com.rep.service.logica.UsuarioMigrationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.net.URL;
import java.nio.file.Paths;
@SpringBootApplication
@EnableJpaAuditing
public class Main extends Application {
	@Autowired
	private UsuarioMigrationService migrationService;
	private ConfigurableApplicationContext springContext;
	private Parent root;

	public static void main(String[] args) {
		// Inicia JavaFX
		launch(args);

	}

	@Override
	public void init() {

		try {
			springContext = new SpringApplicationBuilder(Main.class)
					.headless(false)
					.run();

			// Debug: Verifica rutas de recursos
			URL fxmlUrl = getClass().getResource("/view/Login.fxml");
			if (fxmlUrl == null) {
				System.err.println("No se encontró el archivo FXML. Buscando en:");
				System.err.println(Paths.get("src/main/resources/view/Login.fxml").toAbsolutePath());
				throw new RuntimeException("Archivo FXML no encontrado");
			}
			System.out.println("Archivo FXML encontrado en: " + fxmlUrl);

			SpringFXMLLoader fxmlLoader = springContext.getBean(SpringFXMLLoader.class);
		} catch (Exception e) {
			System.err.println("Error durante la inicialización:");
			e.printStackTrace();
			Platform.exit();
		}
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			// Carga el FXML usando el class loader de Spring
			Resource resource = new ClassPathResource("/view/Login.fxml");
			URL fxmlUrl = resource.getURL();

			SpringFXMLLoader fxmlLoader = springContext.getBean(SpringFXMLLoader.class);
			Parent root = fxmlLoader.load(fxmlUrl);

			// Configuración de la escena
			Scene scene = new Scene(root, 800, 600);

			// Opcional: Carga CSS si lo necesitas
			URL cssUrl = getClass().getResource("/styles/editor.css");
			if (cssUrl != null) {
				scene.getStylesheets().add(cssUrl.toExternalForm());
			}

			primaryStage.setScene(scene);
			primaryStage.setTitle("Mi Aplicación");
			primaryStage.show();

		} catch (Exception e) {
			System.err.println("Error crítico al cargar FXML:");
			e.printStackTrace();

			// Muestra alerta al usuario
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error de Carga");
			alert.setHeaderText("No se pudo cargar la interfaz");
			alert.setContentText(e.getMessage());
			alert.showAndWait();

			Platform.exit();
		}
	}

	@Override
	public void stop() {
		this.springContext.close();
		Platform.exit();
	}

}