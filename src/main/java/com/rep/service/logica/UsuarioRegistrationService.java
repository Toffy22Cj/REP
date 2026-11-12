package com.rep.service.logica;

import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.exception.AccessDeniedException;
import com.rep.exception.ConflictoException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.model.Estudiante;
import com.rep.model.Profesor;
import com.rep.model.Usuario;
import com.rep.repositories.CursoRepository;
import com.rep.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Creación de cuentas de usuario, compartida por el auto-registro público y el panel
 * de administración.
 *
 * <ul>
 *   <li>Auto-registro ({@code POST /api/registro}): solo {@link Usuario.Rol#ESTUDIANTE}.</li>
 *   <li>Administración ({@code POST /api/admin/usuarios}): cualquier rol.</li>
 * </ul>
 * Se crea siempre el subtipo correcto ({@link Estudiante}, {@link Profesor} o
 * {@link Usuario}) y se aplican las reglas de unicidad y de documento de identidad.
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

    /** Auto-registro público: solo estudiantes. */
    @Transactional
    public Usuario registrarEstudiante(RegistroUsuarioDTO dto) {
        if (!Usuario.Rol.ESTUDIANTE.name().equals(dto.getRol())) {
            log.warn("Intento de auto-registro con rol no permitido: {}", dto.getRol());
            throw new AccessDeniedException(ErrorCode.REGISTRO_ROL_NO_PERMITIDO);
        }
        return crear(dto);
    }

    /** Alta de cualquier tipo de usuario (uso exclusivo de administración). */
    @Transactional
    public Usuario crear(RegistroUsuarioDTO dto) {
        verificarUnicidad(dto.getIdentificacion(), dto.getCorreo());
        DocumentoIdentidadPolicy.validar(dto.getTipoIdentificacion(), dto.getIdentificacion(), dto.getEdad());
        return usuarioRepository.save(construirUsuario(dto));
    }

    public void verificarUnicidad(String identificacion, String correo) {
        if (usuarioRepository.existsByIdentificacion(identificacion)) {
            throw new ConflictoException(ErrorCode.USUARIO_IDENTIFICACION_DUPLICADA,
                    Map.of("identificacion", identificacion));
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException(ErrorCode.USUARIO_CORREO_DUPLICADO, Map.of("correo", correo));
        }
    }

    /** Construye la entidad del subtipo adecuado con la contraseña cifrada (sin guardarla). */
    public Usuario construirUsuario(RegistroUsuarioDTO dto) {
        Usuario.Rol rol = parseEnum(Usuario.Rol.class, dto.getRol(), "rol");
        Usuario usuario;
        switch (rol) {
            case ESTUDIANTE -> {
                if (dto.getEdad() == null) {
                    throw new ValidacionException(ErrorCode.ESTUDIANTE_SIN_EDAD);
                }
                if (dto.getCursoId() == null) {
                    throw new ValidacionException(ErrorCode.ESTUDIANTE_SIN_CURSO);
                }
                Estudiante estudiante = new Estudiante();
                estudiante.setCurso(cursoRepository.findById(dto.getCursoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.CURSO_NO_ENCONTRADO, dto.getCursoId())));
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

        usuario.setNombre(dto.getNombre().trim());
        usuario.setApellido(dto.getApellido().trim());
        usuario.setCorreo(dto.getCorreo().trim().toLowerCase());
        usuario.setIdentificacion(dto.getIdentificacion().trim());
        usuario.setTipoIdentificacion(parseEnum(Usuario.TipoIdentificacion.class, dto.getTipoIdentificacion(),
                "tipoIdentificacion"));
        usuario.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        usuario.setRol(rol);
        usuario.setActivo(dto.isActivo());
        usuario.setFechaIngreso(dto.getFechaIngreso());
        usuario.setSexo(dto.getSexo());
        usuario.setEdad(dto.getEdad());
        return usuario;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String campo) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "Valor no válido para '" + campo + "': " + value,
                    Map.of(campo, "Valor no válido: " + value));
        }
    }
}
