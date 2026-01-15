package com.rep;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lanzador principal para la versión standalone de la aplicación.
 * 
 * Esta clase configura automáticamente la aplicación para funcionar
 * sin dependencias externas como MySQL/MariaDB, usando H2 embebida.
 * 
 * @author Sistema Educativo
 * @version 1.0.0-STANDALONE
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // Habilitar tareas programadas (actualizaciones, etc.)
public class StandaloneLauncher {

    private static ConfigurableApplicationContext springContext;
    static final String APP_NAME = "Sistema Educativo REP";
    static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  " + APP_NAME + " - Standalone      ║");
        System.out.println("║  Versión: " + VERSION + "                      ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();

        try {
            // Configurar entorno standalone
            setupStandaloneEnvironment();

            // 1. Iniciar Spring Boot en segundo plano
            startSpringServer(args);

            // 2. Iniciar JavaFX (bloqueante)
            Application.launch(JavaFXStandaloneApp.class, args);

        } catch (Exception e) {
            System.err.println("❌ Error crítico al iniciar la aplicación");
            System.err.println("   Detalles: " + e.getMessage());
            e.printStackTrace();

            showErrorDialog("Error de inicio",
                    "No se pudo iniciar la aplicación: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Configura las propiedades del sistema para el modo standalone.
     * Establece la base de datos H2 embebida y otras configuraciones.
     */
    private static void setupStandaloneEnvironment() {
        System.out.println("⚙️  Configurando entorno standalone...");

        // Base de datos H2 embebida
        System.setProperty("spring.datasource.url",
                "jdbc:h2:file:./data/sistema_educativo;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;AUTO_SERVER=TRUE");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        // Dialecto de Hibernate para H2
        System.setProperty("spring.jpa.properties.hibernate.dialect",
                "org.hibernate.dialect.H2Dialect");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "update");

        // Habilitar consola H2
        System.setProperty("spring.h2.console.enabled", "true");
        System.setProperty("spring.h2.console.path", "/h2-console");

        // Puerto del servidor (cambiar a 18080 para evitar conflictos)
        System.setProperty("server.port", "18080");
        System.setProperty("server.address", "127.0.0.1");

        // Logging
        System.setProperty("logging.file.name", "./logs/application.log");
        System.setProperty("logging.level.root", "INFO");
        System.setProperty("logging.level.com.rep", "DEBUG");

        // Perfil activo
        System.setProperty("spring.profiles.active", "standalone");

        // Información de configuración
        System.out.println("   ✓ Base de datos: H2 embebida");
        System.out.println("   ✓ Archivo DB: ./data/sistema_educativo.mv.db");
        System.out.println("   ✓ Puerto servidor: 18080");
        System.out.println("   ✓ Logs: ./logs/application.log");
        System.out.println();
    }

    /**
     * Inicia el servidor Spring Boot en un hilo separado.
     */
    private static void startSpringServer(String[] args) {
        new Thread(() -> {
            try {
                System.out.println("🚀 Iniciando servidor Spring Boot...");

                springContext = new SpringApplicationBuilder(StandaloneLauncher.class)
                        .headless(false)
                        .run(args);

                System.out.println("✅ Servidor Spring Boot iniciado correctamente");
                System.out.println();
                System.out.println("📊 Accesos disponibles:");
                System.out.println("   • Consola H2: http://localhost:18080/h2-console");
                System.out.println("   • API Docs: http://localhost:18080/swagger-ui.html");
                System.out.println("   • Health: http://localhost:18080/actuator/health");
                System.out.println();
                System.out.println("═══════════════════════════════════════");
                System.out.println();

            } catch (Exception e) {
                System.err.println("❌ Error al iniciar el servidor Spring Boot");
                e.printStackTrace();
                showErrorDialog("Error del servidor",
                        "No se pudo iniciar el servidor: " + e.getMessage());
            }
        }, "Spring-Boot-Thread").start();
    }

    /**
     * Muestra un diálogo de error si JavaFX está disponible.
     */
    private static void showErrorDialog(String title, String message) {
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText("Error de Inicio");
                alert.setContentText(message);
                alert.showAndWait();
            });
        } catch (Exception e) {
            // Si JavaFX no está disponible, solo mostrar en consola
            System.err.println("ERROR: " + title + " - " + message);
        }
    }

    /**
     * Obtiene el contexto de Spring (usado por JavaFXStandaloneApp).
     */
    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}

/**
 * Aplicación JavaFX que se integra con Spring Boot.
 * Usa el NavigationService existente para mantener compatibilidad.
 */
class JavaFXStandaloneApp extends Application {

    private static final int SPRING_STARTUP_WAIT_MS = 2000;

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🎨 Iniciando interfaz JavaFX...");

            // Esperar a que Spring Boot inicie
            waitForSpringBoot();

            // Obtener el contexto de Spring
            ConfigurableApplicationContext context = StandaloneLauncher.getSpringContext();
            if (context == null) {
                throw new RuntimeException("El contexto de Spring no está disponible");
            }

            // Obtener el NavigationService desde Spring
            com.rep.service.fx.NavigationService navigationService = context
                    .getBean(com.rep.service.fx.NavigationService.class);

            // Configurar el stage principal
            navigationService.setPrimaryStage(primaryStage);

            // Configurar propiedades de la ventana
            primaryStage.setTitle(StandaloneLauncher.APP_NAME + " v" + StandaloneLauncher.VERSION);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);

            // Cargar pantalla de inicio (Login)
            navigationService.navigateTo("/view/Login.fxml");

            // Configurar cierre limpio de la aplicación
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("🔄 Cerrando aplicación...");
                stop();
                Platform.exit();
                System.exit(0);
            });

            // Mostrar la ventana
            primaryStage.show();

            System.out.println("✅ Interfaz JavaFX iniciada correctamente");
            System.out.println();
            showStartupInfo();

        } catch (Exception e) {
            System.err.println("❌ Error al iniciar la interfaz JavaFX");
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Interfaz");
                alert.setHeaderText("No se pudo cargar la interfaz gráfica");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                Platform.exit();
                System.exit(1);
            });
        }
    }

    /**
     * Espera a que Spring Boot inicie completamente.
     */
    private void waitForSpringBoot() throws InterruptedException {
        System.out.println("⏳ Esperando a que Spring Boot inicie...");
        Thread.sleep(SPRING_STARTUP_WAIT_MS);
    }

    /**
     * Muestra información de inicio en la consola.
     */
    private void showStartupInfo() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  ✅ APLICACIÓN INICIADA CORRECTAMENTE ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📂 Estructura de archivos:");
        System.out.println("   • Base de datos: ./data/sistema_educativo.mv.db");
        System.out.println("   • Logs: ./logs/application.log");
        System.out.println("   • Backups: ./backups/");
        System.out.println();
        System.out.println("🌐 Servicios web:");
        System.out.println("   • Consola H2: http://localhost:18080/h2-console");
        System.out.println("   • API Docs: http://localhost:18080/swagger-ui.html");
        System.out.println("   • Health: http://localhost:18080/actuator/health");
        System.out.println();
        System.out.println("💡 Credenciales consola H2:");
        System.out.println("   • URL: jdbc:h2:file:./data/sistema_educativo");
        System.out.println("   • Usuario: sa");
        System.out.println("   • Contraseña: (dejar vacío)");
        System.out.println();
        System.out.println("═══════════════════════════════════════");
    }

    @Override
    public void stop() {
        ConfigurableApplicationContext context = StandaloneLauncher.getSpringContext();
        if (context != null && context.isRunning()) {
            System.out.println("🛑 Cerrando servidor Spring Boot...");
            context.close();
        }
        System.out.println("👋 Aplicación finalizada. ¡Hasta pronto!");
    }
}
