package com.rep.red;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Anuncio mDNS/DNS-SD del servidor como {@value #TIPO}: lo encuentran los sistemas que
 * no permiten broadcast UDP a las apps (iOS) y las herramientas estándar (Bonjour en
 * macOS/Windows, NSD en Android, avahi en Linux). Complementa al descubrimiento UDP.
 *
 * TXT: {@code id} (identidad permanente), {@code version}, {@code api=/api}.
 */
@Service
@Slf4j
public class MdnsService {
    public static final String TIPO = "_rep._tcp.local.";

    private final RedConfigService red;
    private final boolean activo;
    private final List<JmDNS> anunciadores = new CopyOnWriteArrayList<>();

    public MdnsService(RedConfigService red, @Value("${app.red.mdns.activo:true}") boolean activo) {
        this.red = red;
        this.activo = activo;
    }

    public boolean anunciando() {
        return !anunciadores.isEmpty();
    }

    /** En segundo plano: el sondeo mDNS tarda unos segundos por interfaz. */
    public void iniciar() {
        if (!activo) {
            return;
        }
        Thread t = new Thread(this::registrar, "rep-mdns");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void detener() {
        for (JmDNS j : anunciadores) {
            try {
                j.unregisterAllServices();
                j.close();
            } catch (IOException e) {
                log.debug("Cerrando mDNS: {}", e.getMessage());
            }
        }
        anunciadores.clear();
    }

    private synchronized void registrar() {
        detener();
        Map<String, String> txt = new LinkedHashMap<>();
        txt.put("id", red.id());
        txt.put("version", red.version());
        txt.put("api", "/api");
        for (InetAddress direccion : direcciones()) {
            try {
                JmDNS jmdns = JmDNS.create(direccion, "rep-" + red.id().substring(0, 8));
                ServiceInfo servicio = ServiceInfo.create(TIPO, red.nombre(), red.puertoHttp(), 0, 0, txt);
                jmdns.registerService(servicio);
                anunciadores.add(jmdns);
            } catch (IOException | RuntimeException e) {
                log.debug("mDNS no disponible en {}: {}", direccion, e.getMessage());
            }
        }
        if (anunciadores.isEmpty()) {
            log.info("mDNS: sin interfaces de red; los clientes usarán el descubrimiento UDP o la dirección");
        } else {
            log.info("mDNS: «{}» anunciado como {} en {} interfaz(es)", red.nombre(), TIPO, anunciadores.size());
        }
    }

    private List<InetAddress> direcciones() {
        List<InetAddress> lista = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) continue;
                if (RedConfigService.clasificar(ni.getName(), ni.getDisplayName()) == RedConfigService.TipoInterfaz.VIRTUAL) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) lista.add(a);
                }
            }
        } catch (SocketException e) {
            log.debug("Sin interfaces para mDNS: {}", e.getMessage());
        }
        return lista;
    }
}
