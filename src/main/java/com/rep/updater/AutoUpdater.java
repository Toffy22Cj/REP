package com.rep.updater;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AutoUpdater {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignorar error de look and feel
        }

        boolean silent = args.length > 0 && args[0].equals("--silent");

        if (silent) {
            checkUpdatesSilent();
        } else {
            SwingUtilities.invokeLater(AutoUpdater::showUpdateInterface);
        }
    }

    private static void showUpdateInterface() {
        JFrame frame = new JFrame("Actualizador Sistema Educativo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 350);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("🔄 Sistema de Actualizaciones");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea infoArea = new JTextArea(8, 40);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText("Buscando actualizaciones...");

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JButton checkButton = new JButton("🔍 Buscar Actualizaciones");
        JButton updateButton = new JButton("⬇️ Descargar e Instalar");
        JButton continueButton = new JButton("➡️ Continuar al Programa");

        updateButton.setEnabled(false);
        updateButton.setVisible(false);
        continueButton.setVisible(true);

        checkButton.addActionListener(e -> {
            checkButton.setEnabled(false);
            progressBar.setVisible(true);
            infoArea.setText("Conectando con GitHub...");

            new Thread(() -> {
                try {
                    GitHubUpdateService.ReleaseInfo latest = GitHubUpdateService.getLatestRelease();

                    String currentVersion = getCurrentVersion();

                    SwingUtilities.invokeLater(() -> {
                        if (latest == null) {
                            infoArea.setText("✅ No hay versiones publicadas todavía en GitHub.\n" +
                                    "Puedes continuar usando el programa normalmente.");
                            continueButton.setText("🚀 Iniciar Programa");
                        } else if (isNewerVersion(latest.getVersion(), currentVersion)) {
                            infoArea.setText(
                                    "¡Actualización disponible!\n\n" +
                                            "Versión actual: " + currentVersion + "\n" +
                                            "Nueva versión: " + latest.getVersion() + "\n\n" +
                                            "Novedades:\n" + latest.getReleaseNotes());
                            updateButton.setVisible(true);
                            updateButton.setEnabled(true);
                            continueButton.setText("🆕 Saltar e Iniciar");
                        } else {
                            infoArea.setText("✅ Ya tienes la última versión (" + currentVersion + ").");
                            continueButton.setText("🚀 Iniciar Programa");
                        }
                        progressBar.setVisible(false);
                        checkButton.setEnabled(true);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        infoArea.setText("❌ Error: " + ex.getMessage());
                        progressBar.setVisible(false);
                        checkButton.setEnabled(true);
                    });
                    ex.printStackTrace();
                }
            }).start();
        });

        updateButton.addActionListener(e -> {
            updateButton.setEnabled(false);
            JOptionPane.showMessageDialog(frame,
                    "La descarga automática e instalación está siendo configurada.\n" +
                            "Por ahora, por favor descargue el archivo manualmente desde GitHub.",
                    "Funcionalidad Proximamente", JOptionPane.INFORMATION_MESSAGE);
        });

        continueButton.addActionListener(e -> {
            frame.dispose();
            // Aquí podríamos llamar a la ejecución de app.jar si fuera necesario,
            // pero si este updater se lanza desde el app.jar, simplemente cerramos.
            // Si se lanza solo, intentamos abrir el app.jar:
            launchMainApp();
            System.exit(0);
        });

        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(new JScrollPane(infoArea));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(progressBar);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(checkButton);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(updateButton);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(continueButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Auto-check on start
        checkButton.doClick();
    }

    private static void launchMainApp() {
        try {
            // Lista de posibles nombres del JAR principal
            String[] commonJarNames = {
                    "app.jar",
                    "sistema-educativo-full.jar",
                    "sistema-educativo-rep.jar"
            };

            File appJar = null;
            for (String jarName : commonJarNames) {
                File f = new File(jarName);
                if (f.exists()) {
                    // Evitar que el actualizador se intente lanzar a sí mismo
                    // si por alguna razón el JAR se llama igual que uno de la lista
                    if (jarName.equalsIgnoreCase("AutoUpdater.jar"))
                        continue;

                    appJar = f;
                    break;
                }
            }

            if (appJar != null) {
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", appJar.getName());
                pb.start();
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                            "❌ No se encontró el archivo ejecutable del programa (app.jar).\n" +
                                    "Por favor, asegúrese de que el actualizador esté en la misma carpeta que la aplicación.",
                            "Error al iniciar", JOptionPane.ERROR_MESSAGE);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "❌ Error al iniciar la aplicación principal: " + e.getMessage(),
                        "Error Crítico", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private static void checkUpdatesSilent() {
        try {
            GitHubUpdateService.ReleaseInfo latest = GitHubUpdateService.getLatestRelease();

            String currentVersion = getCurrentVersion();

            if (isNewerVersion(latest.getVersion(), currentVersion)) {
                StringBuilder content = new StringBuilder();
                content.append("Nueva versión ").append(latest.getVersion()).append(" disponible\n");
                content.append("Fecha: ").append(latest.getPublishedAt()).append("\n");
                content.append("Descargar: ").append(latest.getDownloadUrl());

                Files.writeString(Paths.get(UpdateConfig.UPDATE_FLAG_FILE), content.toString());
                System.out.println("📢 Actualización disponible: " + latest.getVersion());
            }
        } catch (Exception e) {
            // Silencio en modo silent
        }
    }

    private static String getCurrentVersion() {
        try {
            File versionFile = new File(UpdateConfig.VERSION_FILE);
            if (versionFile.exists()) {
                return Files.readString(versionFile.toPath()).trim();
            }
        } catch (Exception e) {
            // Ignorar
        }
        return "1.0.0"; // Versión por defecto
    }

    private static boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null)
            return false;
        return !latest.equals(current);
    }
}
