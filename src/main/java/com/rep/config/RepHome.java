package com.rep.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Directorio de datos del servidor instalado ({@code REP_HOME}, por defecto
 * {@code ~/.rep}). Guarda lo que debe sobrevivir a reinicios y actualizaciones sin
 * pedir nada al usuario: el secreto JWT, la identidad del servidor, la configuración
 * de red y (en la instalación preconfigurada) la base de datos y los archivos.
 */
public final class RepHome {
    private RepHome() {
    }

    public static Path directorio() {
        String desdeEntorno = System.getenv("REP_HOME");
        String desdePropiedad = System.getProperty("rep.home");
        String ruta = desdePropiedad != null && !desdePropiedad.isBlank() ? desdePropiedad
                : desdeEntorno != null && !desdeEntorno.isBlank() ? desdeEntorno
                : System.getProperty("user.home") + "/.rep";
        Path dir = Path.of(ruta);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de datos " + dir, e);
        }
        return dir;
    }

    /** Lee un valor guardado o lo crea la primera vez (archivo legible solo por el usuario). */
    public static synchronized String leerOCrear(String archivo, Supplier<String> generador) {
        Path ruta = directorio().resolve(archivo);
        try {
            if (Files.exists(ruta)) {
                String valor = Files.readString(ruta, StandardCharsets.UTF_8).trim();
                if (!valor.isEmpty()) {
                    return valor;
                }
            }
            String valor = generador.get();
            Files.writeString(ruta, valor, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.setPosixFilePermissions(ruta, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignorada) {
                // Windows: el directorio del usuario ya restringe el acceso
            }
            return valor;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer o crear " + ruta, e);
        }
    }

    public static String secretoJwt() {
        return leerOCrear("jwt.secret", () -> {
            byte[] bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        });
    }

    public static String idServidor() {
        return leerOCrear("servidor.id", () -> UUID.randomUUID().toString());
    }
}
