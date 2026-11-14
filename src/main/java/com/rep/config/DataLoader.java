package com.rep.config;

import com.rep.model.Usuario;
import com.rep.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Crea el administrador inicial si no existe.
 *
 * <p>La contraseña se toma de {@code app.bootstrap.admin-password} (variable
 * {@code ADMIN_INITIAL_PASSWORD}). Si no se define, se genera una aleatoria y se
 * muestra una única vez en el log: ya no existe una contraseña fija conocida
 * ("admin").</p>
 */
@Component
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminIdentificacion;
    private final String adminPassword;
    private final com.rep.service.logica.PrimerAccesoService primerAcceso;
    private final String puerto;

    public DataLoader(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin-identificacion:22}") String adminIdentificacion,
            @Value("${app.bootstrap.admin-password:}") String adminPassword,
            com.rep.service.logica.PrimerAccesoService primerAcceso,
            @Value("${server.port:8080}") String puerto) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminIdentificacion = adminIdentificacion;
        this.adminPassword = adminPassword;
        this.primerAcceso = primerAcceso;
        this.puerto = puerto;
    }

    @Override
    public void run(String... args) {
        boolean generada = adminPassword == null || adminPassword.isBlank();
        Usuario existente = usuarioRepository.findByIdentificacion(adminIdentificacion).orElse(null);

        if (existente != null) {
            // Si nadie cambió aún la contraseña provisional, se genera otra en cada arranque
            // para poder mostrarla de nuevo (la anterior no se guardó en ningún sitio).
            if (existente.requiereCambioDeContrasena() && generada) {
                String password = generarPassword();
                existente.setContraseña(passwordEncoder.encode(password));
                usuarioRepository.save(existente);
                anunciar(password);
            } else {
                log.info("Admin con identificación {} ya existe, no se crea.", adminIdentificacion);
            }
            return;
        }

        String password = generada ? generarPassword() : adminPassword;
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellido("Sistema"); // campo obligatorio: sin él la creación fallaba siempre
        admin.setCorreo("admin@local.test");
        admin.setIdentificacion(adminIdentificacion);
        admin.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
        admin.setContraseña(passwordEncoder.encode(password));
        admin.setRol(Usuario.Rol.ADMIN);
        admin.setActivo(true);
        // Una contraseña generada es provisional: el administrador debe cambiarla al entrar
        admin.setDebeCambiarContrasena(generada);
        usuarioRepository.save(admin);

        if (generada) {
            anunciar(password);
        } else {
            log.info("Administrador inicial creado (identificacion={})", adminIdentificacion);
        }
    }

    private void anunciar(String password) {
        primerAcceso.registrar(adminIdentificacion, password);
        log.warn("""

                ================================================================
                 PRIMER ACCESO AL PANEL DE ADMINISTRACIÓN
                   Usuario:     {}
                   Contraseña:  {}
                 Abra http://localhost:{}/admin/ en este equipo: la contraseña se
                 muestra allí una sola vez y deberá cambiarla al entrar.
                ================================================================""",
                adminIdentificacion, password, puerto);
    }

    private static String generarPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
