package com.rep.config;

import com.rep.model.Usuario;
import com.rep.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            String identificacion = "22";
            if (usuarioRepository.existsByIdentificacion(identificacion)) {
                log.info("Admin con identificación {} ya existe, no se crea.", identificacion);
                return;
            }

            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setCorreo("admin@local.test");
            admin.setIdentificacion(identificacion);
            admin.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
            admin.setContraseña(passwordEncoder.encode("admin"));
            admin.setRol(Usuario.Rol.ADMIN);
            admin.setActivo(true);

            usuarioRepository.save(admin);
            log.info("Usuario administrador creado (identificacion={})", identificacion);
        } catch (Exception e) {
            log.error("Error al crear usuario admin inicial", e);
        }
    }
}
