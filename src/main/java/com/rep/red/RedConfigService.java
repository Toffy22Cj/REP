package com.rep.red;

import com.rep.config.RepHome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Identidad y configuración de red del servidor.
 *
 * <p>Se guarda en {@code REP_HOME/red.properties} para que el nombre y la decisión de
 * anunciarse en la red sobrevivan a reinicios. El identificador ({@code servidor.id})
 * permite a los clientes reconocer el mismo servidor aunque cambie de IP (DHCP, otro
 * hotspot, otra interfaz).</p>
 */
@Service
@Slf4j
public class RedConfigService {

    public enum TipoInterfaz { ETHERNET, WIFI, BLUETOOTH, HOTSPOT, VIRTUAL, OTRA }

    public record Interfaz(String nombre, String descripcion, TipoInterfaz tipo, String ip, String url) {
    }

    public record ClienteVisto(String ip, Instant cuando) {
    }

    private final int puertoHttp;
    private final String version;
    private final Path archivo;
    private final Properties propiedades = new Properties();
    private final Map<String, Instant> clientesRecientes = new ConcurrentHashMap<>();

    public RedConfigService(@Value("${server.port:8080}") int puertoHttp,
            @Value("${app.version:1.0.0}") String version,
            @Value("${app.red.descubrimiento.activo:true}") boolean descubrimientoPorDefecto,
            @Value("${app.red.nombre:}") String nombrePorDefecto) {
        this.puertoHttp = puertoHttp;
        this.version = version;
        this.archivo = RepHome.directorio().resolve("red.properties");
        if (Files.exists(archivo)) {
            try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                propiedades.load(r);
            } catch (IOException e) {
                log.warn("No se pudo leer {}: se usan valores por defecto", archivo);
            }
        }
        propiedades.putIfAbsent("nombre", nombrePorDefecto.isBlank() ? "REP · " + nombreEquipo() : nombrePorDefecto);
        propiedades.putIfAbsent("descubrimiento", String.valueOf(descubrimientoPorDefecto));
    }

    public String id() {
        return RepHome.idServidor();
    }

    public String nombre() {
        return propiedades.getProperty("nombre");
    }

    public boolean descubrimientoActivo() {
        return Boolean.parseBoolean(propiedades.getProperty("descubrimiento"));
    }

    public int puertoHttp() {
        return puertoHttp;
    }

    public String version() {
        return version;
    }

    public synchronized void actualizar(String nombre, Boolean descubrimiento) {
        if (nombre != null && !nombre.isBlank()) {
            propiedades.setProperty("nombre", nombre.trim());
        }
        if (descubrimiento != null) {
            propiedades.setProperty("descubrimiento", descubrimiento.toString());
        }
        try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            propiedades.store(w, "Configuración de red de REP (se edita desde el panel web)");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la configuración de red en " + archivo, e);
        }
    }

    /** Direcciones IPv4 por las que los clientes pueden conectarse a este equipo. */
    public List<Interfaz> interfaces() {
        List<Interfaz> lista = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress dir : ni.getInterfaceAddresses()) {
                    if (dir.getAddress() instanceof Inet4Address ip) {
                        String host = ip.getHostAddress();
                        lista.add(new Interfaz(ni.getName(), ni.getDisplayName(), clasificar(ni.getName(), ni.getDisplayName()),
                                host, "http://" + host + ":" + puertoHttp));
                    }
                }
            }
        } catch (SocketException e) {
            log.warn("No se pudieron listar las interfaces de red: {}", e.getMessage());
        }
        // Primero las útiles para los clientes; las virtuales (docker, VPN…) al final
        lista.sort(Comparator.comparing((Interfaz i) -> i.tipo() == TipoInterfaz.VIRTUAL).thenComparing(Interfaz::nombre));
        return lista;
    }

    /** Direcciones de broadcast de cada red conectada (para anunciarse en todas a la vez). */
    public List<InetAddress> direccionesBroadcast() {
        List<InetAddress> lista = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress dir : ni.getInterfaceAddresses()) {
                    if (dir.getBroadcast() != null) lista.add(dir.getBroadcast());
                }
            }
        } catch (SocketException e) {
            log.debug("Sin interfaces para anunciar: {}", e.getMessage());
        }
        return lista;
    }

    static TipoInterfaz clasificar(String nombre, String descripcion) {
        String t = (nombre + " " + (descripcion == null ? "" : descripcion)).toLowerCase(Locale.ROOT);
        if (t.matches(".*(docker|veth|br-|virbr|vmnet|vbox|virtualbox|hyper-v|tun|tap|tailscale|zerotier|wg\\d).*")) return TipoInterfaz.VIRTUAL;
        if (t.matches(".*(bnep|bluetooth|bt-pan|btpan).*")) return TipoInterfaz.BLUETOOTH;
        if (t.matches(".*(wi-fi direct|hosted network|hotspot|ap\\d|uap|local area connection\\*).*")) return TipoInterfaz.HOTSPOT;
        if (t.matches(".*(wlan|wlp|wifi|wi-fi|wireless|802\\.11).*")) return TipoInterfaz.WIFI;
        if (t.matches(".*(eth|enp|eno|ens|en\\d|ethernet|realtek|intel.*gigabit).*")) return TipoInterfaz.ETHERNET;
        return TipoInterfaz.OTRA;
    }

    void registrarCliente(String ip) {
        if (clientesRecientes.size() > 200) {
            Instant limite = Instant.now().minusSeconds(3600);
            clientesRecientes.values().removeIf(t -> t.isBefore(limite));
        }
        clientesRecientes.put(ip, Instant.now());
    }

    public List<ClienteVisto> clientesRecientes() {
        return clientesRecientes.entrySet().stream()
                .map(e -> new ClienteVisto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ClienteVisto::cuando).reversed())
                .limit(50).toList();
    }

    private static String nombreEquipo() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "servidor";
        }
    }
}
