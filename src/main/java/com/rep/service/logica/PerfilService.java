package com.rep.service.logica;

import com.rep.dto.perfil.ActualizarPerfilDTO;
import com.rep.dto.perfil.PerfilDTO;
import com.rep.exception.ConflictoException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.model.Estudiante;
import com.rep.model.Usuario;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Perfil del usuario autenticado (cualquier rol): consulta, actualización de datos
 * personales y cambio de contraseña.
 */
@Service
@Transactional
public class PerfilService {
    static final int PASSWORD_MIN = 8;

    private final UsuarioRepository usuarioRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditoriaService auditoria;
    private final PrimerAccesoService primerAcceso;

    public PerfilService(UsuarioRepository usuarioRepository, ProfesorMateriaRepository profesorMateriaRepository,
            PasswordEncoder passwordEncoder, AuthService authService, AuditoriaService auditoria,
            PrimerAccesoService primerAcceso) {
        this.usuarioRepository = usuarioRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditoria = auditoria;
        this.primerAcceso = primerAcceso;
    }

    @Transactional(readOnly = true)
    public PerfilDTO obtener(Long usuarioId) {
        Usuario u = cargar(usuarioId);
        String curso = u instanceof Estudiante e && e.getCurso() != null ? e.getCurso().getNombre() : null;
        List<String> asignaciones = u.getRol() == Usuario.Rol.PROFESOR
                ? profesorMateriaRepository.findByProfesorId(u.getId()).stream()
                        .map(pm -> pm.getMateria().getNombre() + " · " + pm.getCurso().getNombre())
                        .sorted().toList()
                : List.of();
        return new PerfilDTO(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(), u.getIdentificacion(),
                u.getTipoIdentificacion() != null ? u.getTipoIdentificacion().name() : null,
                u.getRol().name(), u.getEdad(), u.getSexo() != null ? u.getSexo().name() : null,
                u.getFechaIngreso(), curso, asignaciones, u.requiereCambioDeContrasena());
    }

    public PerfilDTO actualizar(Long usuarioId, ActualizarPerfilDTO dto) {
        Usuario u = cargar(usuarioId);
        String correo = dto.correo().trim().toLowerCase();
        if (!correo.equalsIgnoreCase(u.getCorreo()) && usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException(ErrorCode.USUARIO_CORREO_DUPLICADO, Map.of("correo", correo));
        }
        u.setNombre(dto.nombre().trim());
        u.setApellido(dto.apellido().trim());
        u.setCorreo(correo);
        u.setSexo(dto.sexo());
        usuarioRepository.save(u);
        auditoria.registrar("ACTUALIZAR_PERFIL", "Usuario " + u.getId());
        return obtener(usuarioId);
    }

    /**
     * Cambia la contraseña tras verificar la actual. Invalida los tokens emitidos antes
     * del cambio y devuelve uno nuevo para que la sesión actual continúe.
     */
    public String cambiarContrasena(Long usuarioId, String actual, String nueva) {
        Usuario u = cargar(usuarioId);
        if (!passwordEncoder.matches(actual, u.getContraseña())) {
            throw new ValidacionException(ErrorCode.CONTRASENA_ACTUAL_INCORRECTA, null, Map.of("actual", "Incorrecta"));
        }
        validarFortaleza(nueva, u);
        if (passwordEncoder.matches(nueva, u.getContraseña())) {
            throw new ValidacionException(ErrorCode.CONTRASENA_REPETIDA, null, Map.of("nueva", "Igual a la actual"));
        }
        u.setContraseña(passwordEncoder.encode(nueva));
        u.setCredencialesActualizadasEn(LocalDateTime.now());
        u.setDebeCambiarContrasena(false); // la contraseña provisional deja de existir
        primerAcceso.olvidar(u.getIdentificacion());
        usuarioRepository.save(u);
        auditoria.registrar("CAMBIAR_CONTRASENA", "Usuario " + u.getId());
        return authService.generateToken(u);
    }

    /** Mínimo 8 caracteres, al menos una letra y un número, y distinta de la identificación. */
    static void validarFortaleza(String nueva, Usuario u) {
        String motivo = null;
        if (nueva == null || nueva.length() < PASSWORD_MIN) {
            motivo = "Debe tener al menos " + PASSWORD_MIN + " caracteres";
        } else if (nueva.chars().noneMatch(Character::isLetter) || nueva.chars().noneMatch(Character::isDigit)) {
            motivo = "Debe combinar letras y números";
        } else if (nueva.equalsIgnoreCase(u.getIdentificacion())) {
            motivo = "No puede ser igual a su número de identificación";
        }
        if (motivo != null) {
            throw new ValidacionException(ErrorCode.CONTRASENA_DEBIL, motivo, Map.of("nueva", motivo));
        }
    }

    private Usuario cargar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.USUARIO_NO_ENCONTRADO, id));
    }
}
