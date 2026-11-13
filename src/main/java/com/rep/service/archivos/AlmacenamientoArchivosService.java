package com.rep.service.archivos;

import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.RepException;
import com.rep.exception.ValidacionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento de archivos subidos (adjuntos de preguntas/opciones y material de
 * estudio). Centraliza lo que antes estaba duplicado en cada servicio:
 *
 * <ul>
 *   <li>Lista blanca de extensiones (imágenes, documentos, audio, vídeo, comprimidos).</li>
 *   <li>Nombres internos aleatorios (UUID): el nombre original solo se usa para mostrar.</li>
 *   <li>Rutas relativas a {@code app.storage.dir}; protección contra path traversal.</li>
 *   <li>Respuestas de descarga con el tipo MIME correcto, en línea para imágenes, PDF,
 *       audio y vídeo (se pueden previsualizar) y como adjunto para el resto.</li>
 * </ul>
 */
@Slf4j
@Service
public class AlmacenamientoArchivosService {

    /** Extensión → tipo MIME de los archivos admitidos. */
    public static final Map<String, String> TIPOS_PERMITIDOS = Map.ofEntries(
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"), Map.entry("svg", "image/svg+xml"),
            Map.entry("pdf", "application/pdf"), Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
            Map.entry("mp3", "audio/mpeg"), Map.entry("wav", "audio/wav"), Map.entry("ogg", "audio/ogg"),
            Map.entry("mp4", "video/mp4"), Map.entry("webm", "video/webm"),
            Map.entry("zip", "application/zip"));

    /** Tipos que el navegador o el cliente pueden mostrar directamente. */
    private static final Set<String> PREVISUALIZABLES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf", "text/plain", "audio/mpeg", "audio/wav", "audio/ogg", "video/mp4", "video/webm");

    /** Metadatos del archivo guardado. {@code ruta} es relativa al directorio de almacenamiento. */
    public record ArchivoGuardado(String ruta, String nombreOriginal, String tipoContenido, long tamanoBytes) {
        public boolean esImagen() {
            return tipoContenido != null && tipoContenido.startsWith("image/");
        }
    }

    private final Path raiz;

    public AlmacenamientoArchivosService(@Value("${app.storage.dir:${user.home}/.rep/uploads}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    /**
     * Guarda el archivo en {@code categoria/} (p. ej. "preguntas", "material").
     *
     * @throws ValidacionException con ARCHIVO_VACIO o ARCHIVO_TIPO_NO_PERMITIDO
     */
    public ArchivoGuardado guardar(MultipartFile archivo, String categoria) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException(ErrorCode.ARCHIVO_VACIO);
        }
        String nombreOriginal = nombreSeguro(archivo.getOriginalFilename());
        String extension = extension(nombreOriginal);
        String tipo = TIPOS_PERMITIDOS.get(extension);
        if (tipo == null) {
            throw new ValidacionException(ErrorCode.ARCHIVO_TIPO_NO_PERMITIDO,
                    "Tipo de archivo no permitido (." + extension + "). Permitidos: imágenes, PDF, Office/LibreOffice, "
                            + "texto, audio, vídeo y ZIP",
                    Map.of("extension", extension, "permitidas", String.join(", ", TIPOS_PERMITIDOS.keySet().stream().sorted().toList())));
        }

        String rutaRelativa = categoria + "/" + UUID.randomUUID() + "." + extension;
        Path destino = resolver(rutaRelativa);
        try (InputStream in = archivo.getInputStream()) {
            Files.createDirectories(destino.getParent());
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RepException(ErrorCode.ARCHIVO_NO_GUARDADO, null, null, e);
        }
        log.info("Archivo guardado: {} ({} bytes, {})", rutaRelativa, archivo.getSize(), tipo);
        return new ArchivoGuardado(rutaRelativa, nombreOriginal, tipo, archivo.getSize());
    }

    /** Guarda contenido generado por el servidor (p. ej. datos de demostración). */
    public ArchivoGuardado guardarBytes(byte[] contenido, String nombreOriginal, String categoria) {
        String nombre = nombreSeguro(nombreOriginal);
        String extension = extension(nombre);
        String tipo = TIPOS_PERMITIDOS.get(extension);
        if (tipo == null) {
            throw new ValidacionException(ErrorCode.ARCHIVO_TIPO_NO_PERMITIDO);
        }
        String rutaRelativa = categoria + "/" + UUID.randomUUID() + "." + extension;
        Path destino = resolver(rutaRelativa);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new RepException(ErrorCode.ARCHIVO_NO_GUARDADO, null, null, e);
        }
        return new ArchivoGuardado(rutaRelativa, nombre, tipo, contenido.length);
    }

    /** Elimina un archivo si existe (no falla si ya no está). */
    public void eliminar(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(ruta));
        } catch (IOException | RepException e) {
            log.warn("No se pudo eliminar el archivo {}: {}", ruta, e.getMessage());
        }
    }

    /**
     * Respuesta HTTP de descarga del archivo.
     *
     * @param ruta           ruta relativa (o absoluta, para archivos subidos con versiones antiguas)
     * @param nombreMostrado nombre con el que se descarga
     */
    public ResponseEntity<Resource> descargar(String ruta, String nombreMostrado, String tipoGuardado) {
        if (ruta == null || ruta.isBlank()) {
            throw new RecursoNoEncontradoException(ErrorCode.ARCHIVO_NO_ENCONTRADO);
        }
        Path archivo = resolver(ruta);
        if (!Files.isRegularFile(archivo)) {
            throw new RecursoNoEncontradoException(ErrorCode.ARCHIVO_NO_ENCONTRADO);
        }
        String nombre = nombreMostrado != null && !nombreMostrado.isBlank() ? nombreMostrado : archivo.getFileName().toString();
        String tipo = tipoGuardado != null && !tipoGuardado.isBlank()
                ? tipoGuardado
                : TIPOS_PERMITIDOS.getOrDefault(extension(nombre), MediaType.APPLICATION_OCTET_STREAM_VALUE);

        ContentDisposition disposition = (PREVISUALIZABLES.contains(tipo) ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(nombre, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(archivo));
    }

    /** Ruta absoluta dentro del directorio de almacenamiento (rechaza path traversal). */
    Path resolver(String ruta) {
        Path p = Paths.get(ruta);
        Path absoluta = (p.isAbsolute() ? p : raiz.resolve(p)).normalize();
        // Compatibilidad: versiones anteriores guardaban rutas absolutas dentro de ~/.rep/uploads
        if (!absoluta.startsWith(raiz) && !p.isAbsolute()) {
            throw new ValidacionException(ErrorCode.VALIDACION_FALLIDA, "Ruta de archivo no válida");
        }
        return absoluta;
    }

    static String nombreSeguro(String original) {
        // Sin Paths.get: con un locale del sistema no UTF-8 falla con nombres como "guía.pdf"
        String nombre = original == null ? "" : original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        nombre = nombre.replaceAll("[\\r\\n\"]", "_").trim();
        return nombre.isEmpty() ? "archivo" : nombre;
    }

    static String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
