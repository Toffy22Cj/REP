package com.rep.service.logica;

import com.rep.dto.auth.AuthUsuarioDTO;
import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.model.Estudiante;
import com.rep.model.Usuario;
import com.rep.repositories.CursoRepository;
import com.rep.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-registro público ({@code POST /api/registro}).
 *
 * <p>Por seguridad, el registro público solo puede crear cuentas de
 * {@link Usuario.Rol#ESTUDIANTE}. Antes cualquiera podía registrarse con rol
 * {@code ADMIN} o {@code PROFESOR} y obtener privilegios completos; esas cuentas
 * deben crearse ahora desde el panel de administración ({@code /api/admin/usuarios}).</p>
 *
 * <p>La respuesta devuelve un {@link AuthUsuarioDTO} en lugar de la entidad, para no
 * exponer campos internos.</p>
 */
@Slf4j
@Service
public class UsuarioRegistrationService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursoRepository cursoRepository;

    public UsuarioRegistrationService(UsuarioRepository usuarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      CursoRepository cursoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.cursoRepository = cursoRepository;
    }

    @Transactional
    public ResponseEntity<?> registrarUsuario(RegistroUsuarioDTO dto) {
        if (!Usuario.Rol.ESTUDIANTE.name().equals(dto.getRol())) {
            log.warn("Intento de auto-registro con rol no permitido: {}", dto.getRol());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El registro público solo permite cuentas de estudiante");
        }
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El correo electrónico ya está registrado");
        }
        if (usuarioRepository.existsByIdentificacion(dto.getIdentificacion())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("La identificación ya está registrada");
        }
        if (dto.getEdad() == null) {
            return ResponseEntity.badRequest().body("La edad es obligatoria para estudiantes");
        }
        if (dto.getCursoId() == null) {
            return ResponseEntity.badRequest().body("El curso es obligatorio para estudiantes");
        }

        try {
            Estudiante guardado = usuarioRepository.save(crearEstudiante(dto));
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthUsuarioDTO(guardado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error en los datos: " + e.getMessage());
        }
    }

    private Estudiante crearEstudiante(RegistroUsuarioDTO dto) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(dto.getNombre());
        estudiante.setApellido(dto.getApellido());
        estudiante.setCorreo(dto.getCorreo());
        estudiante.setIdentificacion(dto.getIdentificacion());
        estudiante.setTipoIdentificacion(Usuario.TipoIdentificacion.valueOf(dto.getTipoIdentificacion()));
        estudiante.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        estudiante.setRol(Usuario.Rol.ESTUDIANTE);
        estudiante.setActivo(dto.isActivo());
        estudiante.setFechaIngreso(dto.getFechaIngreso());
        estudiante.setSexo(dto.getSexo());
        estudiante.setEdad(dto.getEdad());
        estudiante.setCurso(cursoRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado")));
        estudiante.setEstado(Estudiante.EstadoEstudiante.activo);
        return estudiante;
    }
}
