package com.rep;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFXStandaloneApp extends Application {

    // Aumentar a 30 segundos máximo
    private static final int MAX_WAIT_ATTEMPTS = 60; // 60 intentos
    private static final int WAIT_INTERVAL_MS = 500; // 500ms entre intentos
    private static final int MAX_WAIT_TIME_MS = MAX_WAIT_ATTEMPTS * WAIT_INTERVAL_MS; // 30 segundos

    @Override
    public void init() {
        // Iniciar Spring Boot en segundo plano DENTRO de init()
        System.out.println("🔄 Iniciando Spring Boot en segundo plano...");
        System.out.println("   Tiempo máximo de espera: " + (MAX_WAIT_TIME_MS / 1000) + " segundos");
        StandaloneLauncher.startSpringServer(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🎨 Iniciando interfaz JavaFX...");
            System.out.println("   Esperando a que Spring Boot esté completamente listo...");

            // Esperar a que Spring Boot inicie COMPLETAMENTE
            waitForSpringBoot();

            // Obtener el contexto de Spring
            ConfigurableApplicationContext context = StandaloneLauncher.getSpringContext();
            if (context == null) {
                throw new RuntimeException("El contexto de Spring no está disponible después de " +
                        (MAX_WAIT_TIME_MS / 1000) + " segundos");
            }

            // Verificar que el contexto esté completamente inicializado
            if (!context.isActive()) {
                throw new RuntimeException("El contexto de Spring no está activo");
            }

            // Verificar que el NavigationService esté disponible
            if (!context.containsBean("navigationService")) {
                throw new RuntimeException("NavigationService no encontrado en Spring context. " +
                        "Beans disponibles: " + String.join(", ", context.getBeanDefinitionNames()));
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
            System.out.println("   Cargando pantalla de Login...");
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
            System.err.println("   Detalles: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Interfaz");
                alert.setHeaderText("No se pudo cargar la interfaz gráfica");
                alert.setContentText(e.getMessage() +
                        "\n\nPosibles soluciones:\n" +
                        "1. Espere unos segundos y reintente\n" +
                        "2. Verifique que el puerto 18080 no esté en uso\n" +
                        "3. Revise logs/application.log\n" +
                        "4. Spring Boot tardó en iniciar (" +
                        (MAX_WAIT_TIME_MS / 1000) + " segundos máximo)");
                alert.showAndWait();
                Platform.exit();
                System.exit(1);
            });
        }
    }

    /**
     * Espera a que Spring Boot inicie completamente.
     * Con reintentos si es necesario.
     */
    private void waitForSpringBoot() throws InterruptedException {
        System.out.println("⏳ Esperando a que Spring Boot inicie...");

        for (int i = 0; i < MAX_WAIT_ATTEMPTS; i++) {
            ConfigurableApplicationContext context = StandaloneLauncher.getSpringContext();

            if (context != null && context.isRunning() && context.isActive()) {
                long elapsedTime = (i + 1) * WAIT_INTERVAL_MS;
                System.out.println("   ✓ Spring Boot listo después de " + elapsedTime + "ms");

                // Esperar un poco más para asegurar que esté completamente listo
                Thread.sleep(1000);
                System.out.println("   ✅ Spring Boot completamente inicializado");
                return;
            }

            // Mostrar progreso cada 5 segundos
            if (i % 10 == 0) { // Cada 5 segundos (10 * 500ms)
                long elapsedSeconds = ((i + 1) * WAIT_INTERVAL_MS) / 1000;
                System.out.println("   ⏰ Esperando... " + elapsedSeconds + " segundos transcurridos");
            }

            Thread.sleep(WAIT_INTERVAL_MS);
        }

        throw new RuntimeException("Timeout: Spring Boot no inició completamente después de " +
                MAX_WAIT_TIME_MS + "ms (" + (MAX_WAIT_TIME_MS / 1000) + " segundos). " +
                "Verifique logs/application.log para detalles.");
    }

    /**
     * Muestra información de inicio en la consola.
     */
    private void showStartupInfo() {
        ConfigurableApplicationContext context = StandaloneLauncher.getSpringContext();
        if (context != null) {
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║  ✅ APLICACIÓN INICIADA CORRECTAMENTE ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println();
            System.out.println("📊 Información del sistema:");
            System.out.println("   • Spring Boot iniciado en: " +
                    context.getStartupDate());
            System.out.println("   • Beans cargados: " +
                    context.getBeanDefinitionCount());
            System.out.println("   • Perfiles activos: " +
                    String.join(", ", context.getEnvironment().getActiveProfiles()));
            System.out.println();
            System.out.println("📂 Estructura de archivos:");
            System.out.println("   • Base de datos: ./data/sistema_educativo.mv.db");
            System.out.println("   • Logs: ./logs/application.log");
            System.out.println("   • Backups: ./backups/");
            System.out.println();
            System.out.println("🌐 Servicios web:");
            System.out.println("   • Aplicación: http://localhost:18080");
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