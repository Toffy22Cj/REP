package com.rep.red;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Descubrimiento automático del servidor en cualquier red IP: LAN cableada, Wi-Fi,
 * hotspot creado por el propio servidor o red Bluetooth (PAN), con o sin internet.
 *
 * <ul>
 *   <li><b>Respuesta</b>: escucha en UDP {@code app.red.descubrimiento.puerto} (47800)
 *       búsquedas por broadcast, multicast ({@value #GRUPO_MULTICAST}) o directas, y
 *       responde al remitente con su identidad.</li>
 *   <li><b>Anuncio</b>: cada pocos segundos publica la misma identidad por broadcast en
 *       cada red conectada y por multicast (puerto 47801), para que los clientes lo vean
 *       sin preguntar.</li>
 * </ul>
 *
 * El cliente construye la URL con la IP desde la que llegó el mensaje, que es la que
 * sabe alcanzar. Mensaje (JSON UTF-8):
 * {@code {"rep":"servidor","v":1,"id":"…","nombre":"…","version":"…","puerto":8080}}.
 */
@Service
@Slf4j
public class DescubrimientoService implements SmartLifecycle {
    public static final String GRUPO_MULTICAST = "239.255.47.80";
    private static final int MAX_RESPUESTAS_POR_SEGUNDO = 50;

    private final RedConfigService red;
    private final ObjectMapper mapper;
    private final int puerto;
    private final int puertoAnuncio;
    private final int intervaloAnuncio;

    private volatile MulticastSocket socket;
    private volatile Thread hilo;
    private ScheduledExecutorService anunciador;
    private final AtomicInteger respuestasEnSegundo = new AtomicInteger();
    private volatile long segundoActual;

    public DescubrimientoService(RedConfigService red, ObjectMapper mapper,
            @Value("${app.red.descubrimiento.puerto:47800}") int puerto,
            @Value("${app.red.anuncio.puerto:47801}") int puertoAnuncio,
            @Value("${app.red.anuncio.intervalo-segundos:5}") int intervaloAnuncio) {
        this.red = red;
        this.mapper = mapper;
        this.puerto = puerto;
        this.puertoAnuncio = puertoAnuncio;
        this.intervaloAnuncio = intervaloAnuncio;
    }

    public int puerto() {
        return socket != null ? socket.getLocalPort() : puerto;
    }

    public boolean escuchando() {
        return socket != null && !socket.isClosed();
    }

    byte[] identidad() {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rep", "servidor");
            m.put("v", 1);
            m.put("id", red.id());
            m.put("nombre", red.nombre());
            m.put("version", red.version());
            m.put("puerto", red.puertoHttp());
            return mapper.writeValueAsBytes(m);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    public synchronized void start() {
        if (!red.descubrimientoActivo() || escuchando()) {
            if (!red.descubrimientoActivo()) log.info("Descubrimiento en red desactivado");
            return;
        }
        try {
            MulticastSocket s = new MulticastSocket(null);
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(puerto));
            s.setBroadcast(true);
            unirseAlGrupo(s);
            socket = s;
        } catch (IOException e) {
            // Otro servidor REP (u otro programa) ya usa el puerto: la app sigue funcionando
            log.warn("No se pudo abrir el puerto UDP {} para el descubrimiento ({}). Los clientes podrán "
                    + "conectarse escribiendo la dirección del servidor.", puerto, e.getMessage());
            return;
        }
        hilo = new Thread(this::escuchar, "rep-descubrimiento");
        hilo.setDaemon(true);
        hilo.start();
        anunciador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rep-anuncio");
            t.setDaemon(true);
            return t;
        });
        anunciador.scheduleAtFixedRate(this::anunciar, 1, Math.max(1, intervaloAnuncio), TimeUnit.SECONDS);
        log.info("Descubrimiento en red activo: «{}» responde en UDP {} y se anuncia en UDP {}", red.nombre(), puerto(),
                puertoAnuncio);
    }

    @Override
    public synchronized void stop() {
        if (anunciador != null) {
            anunciador.shutdownNow();
            anunciador = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
        hilo = null;
    }

    @Override
    public boolean isRunning() {
        return escuchando();
    }

    /** Aplica un cambio de configuración (activar/desactivar) sin reiniciar. */
    public synchronized void reconfigurar() {
        stop();
        start();
    }

    // ------------------------------------------------------------------ respuesta

    private void escuchar() {
        byte[] buffer = new byte[1024];
        MulticastSocket s = socket;
        while (s != null && !s.isClosed()) {
            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
            try {
                s.receive(paquete);
                String texto = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8).trim();
                if (esBusqueda(texto) && dentroDelLimite()) {
                    byte[] respuesta = identidad();
                    s.send(new DatagramPacket(respuesta, respuesta.length, paquete.getSocketAddress()));
                    red.registrarCliente(paquete.getAddress().getHostAddress());
                }
            } catch (IOException e) {
                if (!s.isClosed()) log.debug("Error en el descubrimiento: {}", e.getMessage());
            }
        }
    }

    static boolean esBusqueda(String texto) {
        return texto.equals("REP?") || (texto.startsWith("{") && texto.contains("\"rep\"") && texto.contains("\"buscar\""));
    }

    /** Evita que una avalancha de paquetes (o un abuso) sature la red o el servidor. */
    private boolean dentroDelLimite() {
        long ahora = System.currentTimeMillis() / 1000;
        if (ahora != segundoActual) {
            segundoActual = ahora;
            respuestasEnSegundo.set(0);
        }
        return respuestasEnSegundo.incrementAndGet() <= MAX_RESPUESTAS_POR_SEGUNDO;
    }

    // ------------------------------------------------------------------ anuncio

    private void anunciar() {
        MulticastSocket s = socket;
        if (s == null) return;
        byte[] datos = identidad();
        Set<InetAddress> destinos = new LinkedHashSet<>(red.direccionesBroadcast());
        try {
            destinos.add(InetAddress.getByName("255.255.255.255"));
            destinos.add(InetAddress.getByName(GRUPO_MULTICAST));
        } catch (UnknownHostException ignorada) {
            // direcciones literales: no ocurre
        }
        for (InetAddress destino : destinos) {
            try {
                s.send(new DatagramPacket(datos, datos.length, destino, puertoAnuncio));
            } catch (IOException e) {
                log.trace("No se pudo anunciar en {}: {}", destino, e.getMessage());
            }
        }
    }

    private static void unirseAlGrupo(MulticastSocket s) {
        InetSocketAddress grupo = new InetSocketAddress(GRUPO_MULTICAST, 0);
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isUp() && ni.supportsMulticast() && !ni.isLoopback()) {
                    try {
                        s.joinGroup(grupo, ni);
                    } catch (IOException ignorada) {
                        // interfaz sin IPv4 o sin multicast: se sigue con las demás
                    }
                }
            }
        } catch (SocketException ignorada) {
            // sin interfaces: queda la respuesta a broadcast y a localhost
        }
    }
}
