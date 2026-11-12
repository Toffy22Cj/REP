package com.rep.service.logica;

import com.rep.dto.auth.AuthUsuarioDTO;
import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.model.Estudiante;
import com.rep.model.Profesor;
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

        try {
            Usuario guardado = usuarioRepository.save(construirUsuario(dto));
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthUsuarioDTO(guardado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error en los datos: " + e.getMessage());
        }
    }

    /**
     * Construye la entidad del subtipo correcto según el rol ({@link Estudiante},
     * {@link Profesor} o {@link Usuario} para administradores), con la contraseña
     * cifrada. Usado tanto por el auto-registro como por el panel de administración:
     * antes el panel creaba siempre un {@code Usuario} genérico, por lo que los
     * profesores no podían recibir asignaciones ni los estudiantes un curso.
     *
     * @throws IllegalArgumentException si los datos son inconsistentes
     */
    public Usuario construirUsuario(RegistroUsuarioDTO dto) {
        Usuario.Rol rol = Usuario.Rol.valueOf(dto.getRol());
        Usuario usuario;
        switch (rol) {
            case ESTUDIANTE -> {
                if (dto.getEdad() == null) {
                    throw new IllegalArgumentException("La edad es obligatoria para estudiantes");
                }
                if (dto.getCursoId() == null) {
                    throw new IllegalArgumentException("El curso es obligatorio para estudiantes");
                }
                Estudiante estudiante = new Estudiante();
                estudiante.setCurso(cursoRepository.findById(dto.getCursoId())
                        .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado")));
                estudiante.setEstado(Estudiante.EstadoEstudiante.activo);
                usuario = estudiante;
            }
            case PROFESOR -> {
                Profesor profesor = new Profesor();
                profesor.setEstado(Profesor.EstadoProfesor.activo);
                usuario = profesor;
            }
            default -> usuario = new Usuario();
        }

        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setCorreo(dto.getCorreo());
        usuario.setIdentificacion(dto.getIdentificacion());
        usuario.setTipoIdentificacion(Usuario.TipoIdentificacion.valueOf(dto.getTipoIdentificacion()));
        usuario.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        usuario.setRol(rol);
        usuario.setActivo(dto.isActivo());
        usuario.setFechaIngreso(dto.getFechaIngreso());
        usuario.setSexo(dto.getSexo());
        usuario.setEdad(dto.getEdad());
        return usuario;
    }
}
