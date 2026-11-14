package com.rep.funcional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rep.model.Usuario;
import com.rep.red.DescubrimientoService;
import com.rep.repositories.UsuarioRepository;
import com.rep.service.logica.PrimerAccesoService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Primer acceso del administrador (contraseña provisional mostrada una vez y solo en el
 * propio equipo; cambio obligatorio) y descubrimiento del servidor por UDP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:redprimeracceso;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER",
        "app.red.descubrimiento.activo=true",
        "app.red.descubrimiento.puerto=0",      // puerto libre al azar
        "app.red.anuncio.puerto=47999",
        "app.red.nombre=Servidor de pruebas",
        "app.security.rate-limit.api-per-minute=10000",
        "app.security.rate-limit.login-per-15min=1000"
})
class RedYPrimerAccesoTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SecretKey secretKey;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder encoder;
    @Autowired private PrimerAccesoService primerAcceso;
    @Autowired private DescubrimientoService descubrimiento;

    @Test
    void laContrasenaProvisionalSoloSeMuestraUnaVezYEnElPropioEquipo() throws Exception {
        primerAcceso.registrar("22", "Provisional123");

        // Desde otro equipo de la red: nunca
        mvc.perform(get("/admin/primer-acceso").with(r -> { r.setRemoteAddr("192.168.1.50"); return r; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostrar").value(false))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        // En el propio servidor: una vez
        mvc.perform(get("/admin/primer-acceso").with(r -> { r.setRemoteAddr("127.0.0.1"); return r; }))
                .andExpect(jsonPath("$.mostrar").value(true))
                .andExpect(jsonPath("$.contrasena").value("Provisional123"));
        mvc.perform(get("/admin/primer-acceso").with(r -> { r.setRemoteAddr("127.0.0.1"); return r; }))
                .andExpect(jsonPath("$.mostrar").value(false))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        primerAcceso.olvidar("22");
    }

    @Test
    void conContrasenaProvisionalSoloSePuedeCambiarla() throws Exception {
        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setApellido("Nuevo");
        admin.setCorreo("admin.nuevo@local.test");
        admin.setIdentificacion("7777777");
        admin.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
        admin.setRol(Usuario.Rol.ADMIN);
        admin.setActivo(true);
        admin.setContraseña(encoder.encode("Provisional123"));
        admin.setDebeCambiarContrasena(true);
        usuarios.save(admin);

        // El login informa al cliente que debe cambiarla
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"7777777\",\"password\":\"Provisional123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.debeCambiarContrasena").value(true));

        String token = bearer("7777777", 5);
        mvc.perform(get("/api/admin/usuarios").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAMBIO_CONTRASENA_OBLIGATORIO"));
        mvc.perform(get("/api/admin/perfil").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarContrasena").value(true));

        String json = mvc.perform(put("/api/admin/perfil/contrasena").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"Provisional123\",\"nueva\":\"MiClavePropia2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String nuevoToken = "Bearer " + mapper.readTree(json).get("token").asText();

        mvc.perform(get("/api/admin/usuarios").header("Authorization", nuevoToken))
                .andExpect(status().isOk());
        assertThat(usuarios.findByIdentificacion("7777777").orElseThrow().requiereCambioDeContrasena()).isFalse();
    }

    @Test
    void elServidorRespondeALaBusquedaUdpConSuIdentidad() throws Exception {
        assertThat(descubrimiento.escuchando()).isTrue();
        try (DatagramSocket cliente = new DatagramSocket()) {
            cliente.setSoTimeout(3000);
            byte[] busqueda = "{\"rep\":\"buscar\",\"v\":1}".getBytes(StandardCharsets.UTF_8);
            cliente.send(new DatagramPacket(busqueda, busqueda.length, InetAddress.getLoopbackAddress(),
                    descubrimiento.puerto()));
            DatagramPacket respuesta = new DatagramPacket(new byte[1024], 1024);
            cliente.receive(respuesta);
            JsonNode id = mapper.readTree(new String(respuesta.getData(), 0, respuesta.getLength(), StandardCharsets.UTF_8));
            assertThat(id.get("rep").asText()).isEqualTo("servidor");
            assertThat(id.get("nombre").asText()).isEqualTo("Servidor de pruebas");
            assertThat(id.get("id").asText()).hasSize(36);

            // La misma identidad por HTTP (el cliente la usa para confirmar la dirección)
            mvc.perform(get("/api/public/servidor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.get("id").asText()));
        }
    }

    @Test
    void ignoraMensajesQueNoSonBusquedas() throws Exception {
        try (DatagramSocket cliente = new DatagramSocket()) {
            cliente.setSoTimeout(800);
            byte[] basura = "hola".getBytes(StandardCharsets.UTF_8);
            cliente.send(new DatagramPacket(basura, basura.length, InetAddress.getLoopbackAddress(), descubrimiento.puerto()));
            DatagramPacket respuesta = new DatagramPacket(new byte[1024], 1024);
            org.junit.jupiter.api.Assertions.assertThrows(java.net.SocketTimeoutException.class, () -> cliente.receive(respuesta));
        }
    }

    private String bearer(String identificacion, int segundosAtras) {
        Usuario u = usuarios.findByIdentificacion(identificacion).orElseThrow();
        long ahora = System.currentTimeMillis() - segundosAtras * 1000L;
        return "Bearer " + Jwts.builder().setSubject(identificacion)
                .claim("authorities", List.of(u.getRol().name()))
                .setIssuedAt(new Date(ahora))
                .setExpiration(new Date(ahora + 600_000))
                .signWith(secretKey).compact();
    }
}
