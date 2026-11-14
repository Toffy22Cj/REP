package com.rep.red;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Identidad pública del servidor. El cliente la consulta para confirmar que una
 * dirección es un servidor REP y que es el mismo (por {@code id}) aunque cambie de IP.
 */
@RestController
public class ServidorPublicoApi {
    private final RedConfigService red;

    public ServidorPublicoApi(RedConfigService red) {
        this.red = red;
    }

    @GetMapping("/api/public/servidor")
    public Map<String, Object> identidad() {
        return Map.of("rep", "servidor", "id", red.id(), "nombre", red.nombre(), "version", red.version(),
                "puerto", red.puertoHttp());
    }
}
