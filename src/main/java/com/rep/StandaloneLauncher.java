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

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
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
            // Configurar entorno standalone - DEBE SER LO PRIMERO
            setupStandaloneEnvironment();

            // Iniciar la aplicación JavaFX (que luego inicia Spring)
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
     * ESTO ES CLAVE: Configura las propiedades ANTES de iniciar Spring
     */
    private static void setupStandaloneEnvironment() {
        System.out.println("⚙️  Configurando entorno standalone...");

        // 1. Activar perfil standalone (esto DESACTIVA el perfil por defecto)
        System.setProperty("spring.profiles.active", "standalone");

        // 2. Configurar propiedades CRÍTICAS que deben sobrescribirse
        // (Las demás vienen de application-standalone.properties)

        // Base de datos H2
        System.setProperty("spring.datasource.url",
                "jdbc:h2:file:./data/sistema_educativo;AUTO_SERVER=TRUE");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        // Server - IMPORTANTE: Forzar puerto 18080
        System.setProperty("server.port", "18080");

        // URL de API - Forzar standalone
        System.setProperty("app.api.base-url", "http://localhost:18080/api");
        System.setProperty("auth.service.url", "http://localhost:18080/api/auth");
        System.setProperty("estudiante.service.url", "http://localhost:18080/api");

        // Deshabilitar migración y updates
        System.setProperty("app.migrate-users", "false");
        System.setProperty("app.update-check.enabled", "false");

        // Habilitar override
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");

        System.out.println("   ✓ Perfil standalone activado");
        System.out.println("   ✓ Puerto: 18080");
        System.out.println("   ✓ Base de datos: H2 embebida");
        System.out.println();
    }

    /**
     * Inicia el servidor Spring Boot en un hilo separado.
     * Este método ahora es llamado desde JavaFXStandaloneApp.init()
     */
    public static void startSpringServer(String[] args) {
        new Thread(() -> {
            try {
                System.out.println("🚀 Iniciando servidor Spring Boot...");

                springContext = new SpringApplicationBuilder(StandaloneLauncher.class)
                        .headless(false)
                        .run(args);

                System.out.println("✅ Servidor Spring Boot iniciado correctamente");
                System.out.println();
                System.out.println("📊 Accesos disponibles:");
                System.out.println("   • Aplicación: http://localhost:18080");
                System.out.println("   • Consola H2: http://localhost:18080/h2-console");
                System.out.println("   • API Docs: http://localhost:18080/swagger-ui.html");
                System.out.println();
                System.out.println("💡 Credenciales H2:");
                System.out.println("   • Usuario: sa");
                System.out.println("   • Contraseña: (vacía)");
                System.out.println();
                System.out.println("═══════════════════════════════════════");

            } catch (Exception e) {
                System.err.println("❌ Error al iniciar el servidor Spring Boot");
                e.printStackTrace();
                showErrorDialog("Error del servidor",
                        "No se pudo iniciar el servidor: " + e.getMessage());

                // Mostrar errores comunes y soluciones
                if (e.getMessage() != null && e.getMessage().contains("Port 18080")) {
                    System.err.println("\n⚠️  SOLUCIÓN: El puerto 18080 está en uso.");
                    System.err.println("   Ejecute con otro puerto:");
                    System.err.println("   java -jar app.jar --server.port=18081");
                }

                // Forzar salida si Spring no inicia
                Platform.exit();
                System.exit(1);
            }
        }, "Spring-Boot-Thread").start();
    }

    /**
     * Muestra un diálogo de error si JavaFX está disponible.
     */
    private static void showErrorDialog(String title, String message) {
        try {
            // Usar Platform.runLater para asegurar que se ejecute en el hilo de JavaFX
            if (Platform.isFxApplicationThread()) {
                showAlert(title, message);
            } else {
                Platform.runLater(() -> showAlert(title, message));
            }
        } catch (Exception e) {
            // Si JavaFX no está disponible, solo mostrar en consola
            System.err.println("ERROR: " + title + " - " + message);
        }
    }

    private static void showAlert(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Error de Inicio");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("No se pudo mostrar alerta: " + e.getMessage());
        }
    }

    /**
     * Obtiene el contexto de Spring (usado por JavaFXStandaloneApp).
     */
    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}