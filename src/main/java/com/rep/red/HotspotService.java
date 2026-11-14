package com.rep.red;

import com.rep.exception.ErrorCode;
import com.rep.exception.RepException;
import com.rep.exception.ValidacionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Red Wi-Fi propia del servidor (hotspot) para colegios sin router: el servidor crea la
 * red y los demás equipos se conectan a ella.
 *
 * <ul>
 *   <li><b>Linux</b>: con NetworkManager ({@code nmcli device wifi hotspot}).</li>
 *   <li><b>Windows / macOS</b>: el sistema no ofrece una orden estable para hacerlo;
 *       se devuelven las instrucciones para activarlo desde la configuración.</li>
 * </ul>
 *
 * Las órdenes se ejecutan sin intérprete de comandos y con argumentos validados, de
 * modo que el nombre o la clave no pueden inyectar instrucciones.
 */
@Service
@Slf4j
public class HotspotService {
    private static final Pattern SSID = Pattern.compile("[\\p{L}\\p{N} ._-]{1,32}");
    private static final Pattern CLAVE = Pattern.compile("[\\x21-\\x7E&&[^\"'\\\\`$]]{8,63}");
    private static final String CONEXION = "Hotspot";

    private final boolean permitido;
    private final String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    public HotspotService(@Value("${app.red.hotspot.permitido:true}") boolean permitido) {
        this.permitido = permitido;
    }

    public Map<String, Object> estado() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("permitido", permitido);
        m.put("sistema", sistema.contains("win") ? "windows" : sistema.contains("mac") ? "macos" : "linux");
        boolean soportado = permitido && esLinuxConNmcli();
        m.put("soportado", soportado);
        if (soportado) {
            m.put("activo", hotspotActivo());
        }
        m.put("instrucciones", instrucciones());
        return m;
    }

    public void activar(String ssid, String clave) {
        exigirSoporte();
        if (ssid == null || !SSID.matcher(ssid).matches()) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "Nombre de red no válido",
                    Map.of("ssid", "1 a 32 caracteres: letras, números, espacio, punto, guion"));
        }
        if (clave == null || !CLAVE.matcher(clave).matches()) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "Clave de red no válida",
                    Map.of("clave", "8 a 63 caracteres, sin comillas, \\, ` ni $"));
        }
        ejecutar(List.of("nmcli", "device", "wifi", "hotspot", "con-name", CONEXION, "ssid", ssid, "password", clave));
        log.info("Red Wi-Fi «{}» creada por el servidor", ssid);
    }

    public void desactivar() {
        exigirSoporte();
        ejecutar(List.of("nmcli", "connection", "down", CONEXION));
        log.info("Red Wi-Fi del servidor detenida");
    }

    private void exigirSoporte() {
        if (!permitido || !esLinuxConNmcli()) {
            throw new RepException(ErrorCode.HOTSPOT_NO_SOPORTADO);
        }
    }

    private boolean hotspotActivo() {
        try {
            String salida = ejecutar(List.of("nmcli", "-t", "-f", "NAME", "connection", "show", "--active"));
            return salida.lines().anyMatch(CONEXION::equals);
        } catch (RepException e) {
            return false;
        }
    }

    private boolean esLinuxConNmcli() {
        if (!sistema.contains("linux")) return false;
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, "nmcli").canExecute()) return true;
        }
        return false;
    }

    private static String ejecutar(List<String> orden) {
        try {
            Process p = new ProcessBuilder(orden).redirectErrorStream(true).start();
            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new RepException(ErrorCode.HOTSPOT_FALLO, "El sistema tardó demasiado en responder");
            }
            String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (p.exitValue() != 0) {
                throw new RepException(ErrorCode.HOTSPOT_FALLO,
                        "No se pudo completar la operación: " + (salida.isEmpty() ? "código " + p.exitValue() : salida));
            }
            return salida;
        } catch (IOException e) {
            throw new RepException(ErrorCode.HOTSPOT_FALLO, "No se pudo ejecutar nmcli: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepException(ErrorCode.HOTSPOT_FALLO, "Operación interrumpida");
        }
    }

    private String instrucciones() {
        if (sistema.contains("win")) {
            return "Windows: Configuración › Red e Internet › Zona con cobertura inalámbrica móvil. Active "
                    + "«Compartir mi conexión», elija Wi-Fi, y anote el nombre y la contraseña de red para los demás "
                    + "equipos. Funciona aunque no haya internet.";
        }
        if (sistema.contains("mac")) {
            return "macOS: Ajustes del Sistema › General › Compartir › Compartir Internet. También puede usar un "
                    + "router o punto de acceso.";
        }
        return "Linux: requiere NetworkManager (nmcli) y una tarjeta Wi-Fi que admita modo punto de acceso.";
    }
}
