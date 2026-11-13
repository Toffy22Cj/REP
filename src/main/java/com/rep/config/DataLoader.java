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

    public DataLoader(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin-identificacion:22}") String adminIdentificacion,
            @Value("${app.bootstrap.admin-password:}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminIdentificacion = adminIdentificacion;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByIdentificacion(adminIdentificacion)) {
            log.info("Admin con identificación {} ya existe, no se crea.", adminIdentificacion);
            return;
        }

        boolean generada = adminPassword == null || adminPassword.isBlank();
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
        usuarioRepository.save(admin);

        if (generada) {
            log.warn("Administrador inicial creado (identificacion={}). Contraseña generada: {} "
                    + "-- cámbiela tras el primer acceso o defina ADMIN_INITIAL_PASSWORD.", adminIdentificacion, password);
        } else {
            log.info("Administrador inicial creado (identificacion={})", adminIdentificacion);
        }
    }

    private static String generarPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
