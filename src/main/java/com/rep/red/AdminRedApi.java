package com.rep.red;

import com.rep.service.logica.AuditoriaService;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Configuración de red del servidor (panel web › Red). Solo administradores. */
@RestController
@RequestMapping("/api/admin/red")
public class AdminRedApi {

    public record ActualizarRedDTO(@Size(max = 60, message = "Máximo 60 caracteres") String nombre,
            Boolean descubrimiento) {
    }

    public record HotspotDTO(String ssid, String clave) {
    }

    private final RedConfigService red;
    private final DescubrimientoService descubrimiento;
    private final HotspotService hotspot;
    private final MdnsService mdns;
    private final AuditoriaService auditoria;
    private final int puertoAnuncio;

    public AdminRedApi(RedConfigService red, DescubrimientoService descubrimiento, HotspotService hotspot, MdnsService mdns,
            AuditoriaService auditoria,
            @org.springframework.beans.factory.annotation.Value("${app.red.anuncio.puerto:47801}") int puertoAnuncio) {
        this.red = red;
        this.descubrimiento = descubrimiento;
        this.hotspot = hotspot;
        this.mdns = mdns;
        this.auditoria = auditoria;
        this.puertoAnuncio = puertoAnuncio;
    }

    @GetMapping
    public Map<String, Object> estado() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", red.id());
        m.put("nombre", red.nombre());
        m.put("version", red.version());
        m.put("puertoHttp", red.puertoHttp());
        m.put("descubrimiento", Map.of(
                "activo", red.descubrimientoActivo(),
                "escuchando", descubrimiento.escuchando(),
                "puerto", descubrimiento.puerto(),
                "puertoAnuncio", puertoAnuncio,
                "mdns", mdns.anunciando()));
        m.put("interfaces", red.interfaces());
        m.put("clientesRecientes", red.clientesRecientes());
        m.put("hotspot", hotspot.estado());
        return m;
    }

    @PutMapping
    public Map<String, Object> actualizar(@jakarta.validation.Valid @RequestBody ActualizarRedDTO dto) {
        boolean cambiaDescubrimiento = dto.descubrimiento() != null && dto.descubrimiento() != red.descubrimientoActivo();
        red.actualizar(dto.nombre(), dto.descubrimiento());
        if (cambiaDescubrimiento) {
            descubrimiento.reconfigurar();
        }
        auditoria.registrar("CONFIGURAR_RED", "nombre=" + red.nombre() + ", descubrimiento=" + red.descubrimientoActivo());
        return estado();
    }

    @PostMapping("/hotspot")
    public Map<String, Object> activarHotspot(@RequestBody HotspotDTO dto) {
        hotspot.activar(dto.ssid(), dto.clave());
        auditoria.registrar("ACTIVAR_HOTSPOT", "ssid=" + dto.ssid());
        return estado();
    }

    @DeleteMapping("/hotspot")
    public ResponseEntity<Map<String, Object>> desactivarHotspot() {
        hotspot.desactivar();
        auditoria.registrar("DESACTIVAR_HOTSPOT", "");
        return ResponseEntity.ok(estado());
    }
}
