package com.rep.controller;

import com.rep.service.logica.PrimerAccesoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Muestra la contraseña provisional del administrador en la página de inicio de sesión
 * del panel web, <b>una sola vez</b> y <b>solo</b> a navegadores abiertos en el propio
 * servidor (dirección de bucle local). Desde otro equipo de la red nunca se revela.
 */
@RestController
public class PrimerAccesoController {
    private final PrimerAccesoService primerAcceso;

    public PrimerAccesoController(PrimerAccesoService primerAcceso) {
        this.primerAcceso = primerAcceso;
    }

    @GetMapping("/admin/primer-acceso")
    public Map<String, Object> primerAcceso(HttpServletRequest request) {
        Map<String, Object> r = new LinkedHashMap<>();
        boolean local = esLocal(request.getRemoteAddr());
        r.put("pendiente", primerAcceso.pendiente());
        if (!local) {
            r.put("mostrar", false);
            r.put("motivo", "Abra el panel en el equipo servidor para ver la contraseña inicial");
            return r;
        }
        primerAcceso.revelarUnaVez().ifPresentOrElse(c -> {
            r.put("mostrar", true);
            r.put("identificacion", c.identificacion());
            r.put("contrasena", c.contrasena());
        }, () -> {
            r.put("mostrar", false);
            if (primerAcceso.yaMostrada()) {
                r.put("motivo", "La contraseña inicial ya se mostró. Si no la guardó, reinicie el servidor para generar otra.");
            }
        });
        return r;
    }

    static boolean esLocal(String ip) {
        try {
            return ip != null && InetAddress.getByName(ip).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
