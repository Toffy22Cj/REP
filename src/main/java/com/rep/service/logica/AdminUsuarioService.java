package com.rep.service.logica;

import com.rep.dto.auth.ActualizarUsuarioDTO;
import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.exception.*;
import com.rep.model.Curso;
import com.rep.model.Estudiante;
import com.rep.model.Profesor;
import com.rep.model.Usuario;
import com.rep.repositories.CursoRepository;
import com.rep.repositories.EstudianteRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.ProfesorRepository;
import com.rep.repositories.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Gestión de usuarios desde la administración. */
@Service
@Transactional
public class AdminUsuarioService {
    private static final int PASSWORD_MIN = 8;

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRegistrationService registrationService;
    private final AuditoriaService auditoria;
    private final PasswordEncoder passwordEncoder;

    public AdminUsuarioService(UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            ProfesorRepository profesorRepository,
            ProfesorMateriaRepository profesorMateriaRepository,
            CursoRepository cursoRepository,
            UsuarioRegistrationService registrationService,
            AuditoriaService auditoria,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.cursoRepository = cursoRepository;
        this.registrationService = registrationService;
        this.auditoria = auditoria;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar(String rol) {
        if (rol == null || rol.isBlank()) {
            return usuarioRepository.findAll();
        }
        try {
            return usuarioRepository.findByRol(Usuario.Rol.valueOf(rol));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(ErrorCode.PARAMETRO_INVALIDO, "Rol no válido: " + rol);
        }
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.USUARIO_NO_ENCONTRADO, id));
    }

    public Usuario crear(RegistroUsuarioDTO dto) {
        Usuario guardado = registrationService.crear(dto);
        auditoria.registrar("CREAR_USUARIO", "ID: " + guardado.getId() + ", ROL: " + guardado.getRol());
        return guardado;
    }

    public Usuario actualizar(Long id, ActualizarUsuarioDTO dto) {
        Usuario usuario = obtener(id);

        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre().trim());
        if (dto.getApellido() != null) usuario.setApellido(dto.getApellido().trim());

        if (dto.getCorreo() != null) {
            String correo = dto.getCorreo().trim().toLowerCase();
            if (!correo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreo(correo)) {
                throw new ConflictoException(ErrorCode.USUARIO_CORREO_DUPLICADO, Map.of("correo", correo));
            }
            usuario.setCorreo(correo);
        }

        if (dto.getContraseña() != null && !dto.getContraseña().isBlank()) {
            if (dto.getContraseña().length() < PASSWORD_MIN) {
                throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS,
                        "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres",
                        Map.of("contraseña", "Mínimo " + PASSWORD_MIN + " caracteres"));
            }
            usuario.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        }

        if (dto.getRol() != null) {
            Usuario.Rol nuevoRol = Usuario.Rol.valueOf(dto.getRol());
            if (nuevoRol != usuario.getRol()) {
                // Profesor y Estudiante son tablas propias (herencia JOINED): cambiar solo el
                // campo rol dejaría datos inconsistentes (p. ej. un "estudiante" sin curso).
                boolean tienePerfil = usuario instanceof Profesor || usuario instanceof Estudiante;
                boolean requierePerfil = nuevoRol != Usuario.Rol.ADMIN;
                if (tienePerfil || requierePerfil) {
                    throw new ConflictoException(ErrorCode.USUARIO_CAMBIO_ROL_NO_PERMITIDO,
                            "No se puede cambiar el rol de " + usuario.getRol() + " a " + nuevoRol
                                    + ". Cree una cuenta nueva con el rol deseado");
                }
                usuario.setRol(nuevoRol);
            }
        }

        if (dto.getActivo() != null && !dto.getActivo()) {
            verificarNoEsElMismo(id);
        }
        if (dto.getActivo() != null) usuario.setActivo(dto.getActivo());

        Usuario guardado = usuarioRepository.save(usuario);
        auditoria.registrar("ACTUALIZAR_USUARIO",
                "ID: " + id + ", Nombre: " + guardado.getNombre() + " " + guardado.getApellido());
        return guardado;
    }

    public Usuario cambiarEstado(Long id, boolean activo) {
        Usuario usuario = obtener(id);
        if (!activo) {
            verificarNoEsElMismo(id);
        }
        usuario.setActivo(activo);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoria.registrar("CAMBIAR_ESTADO_USUARIO", "ID: " + id + ", Activo: " + activo);
        return guardado;
    }

    public void eliminar(Long id) {
        Usuario usuario = obtener(id);
        verificarNoEsElMismo(id);
        if (usuario.getRol() == Usuario.Rol.PROFESOR && profesorMateriaRepository.existsByProfesorId(id)) {
            throw new ConflictoException(ErrorCode.PROFESOR_CON_ASIGNACIONES);
        }
        auditoria.registrar("ELIMINAR_USUARIO", "ID: " + id + ", Rol: " + usuario.getRol());
        usuarioRepository.delete(usuario);
    }

    public Estudiante asignarCurso(Long estudianteId, Long cursoId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ESTUDIANTE_NO_ENCONTRADO, estudianteId));
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.CURSO_NO_ENCONTRADO, cursoId));
        if (estudiante.getCurso() != null && estudiante.getCurso().getId().equals(cursoId)) {
            throw new ConflictoException(ErrorCode.ESTUDIANTE_YA_EN_CURSO);
        }
        estudiante.setCurso(curso);
        Estudiante guardado = estudianteRepository.save(estudiante);
        auditoria.registrar("ASIGNAR_CURSO_ESTUDIANTE", "Estudiante: " + estudianteId + ", Curso: " + curso.getNombre());
        return guardado;
    }

    @Transactional(readOnly = true)
    public Profesor obtenerProfesor(Long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.PROFESOR_NO_ENCONTRADO, id));
    }

    public Profesor cambiarEstadoProfesor(Long id, String estado) {
        Profesor profesor = obtenerProfesor(id);
        if (estado == null || estado.isBlank()) {
            throw new ValidacionException(ErrorCode.PROFESOR_ESTADO_INVALIDO);
        }
        try {
            profesor.setEstado(Profesor.EstadoProfesor.valueOf(estado.trim().toLowerCase()));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(ErrorCode.PROFESOR_ESTADO_INVALIDO);
        }
        Profesor guardado = profesorRepository.save(profesor);
        auditoria.registrar("CAMBIAR_ESTADO_PROFESOR", "ID: " + id + ", Estado: " + estado);
        return guardado;
    }

    /** Evita que un administrador se bloquee a sí mismo. */
    private void verificarNoEsElMismo(Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario actual && id.equals(actual.getId())) {
            throw new OperacionNoPermitidaException(ErrorCode.USUARIO_AUTOGESTION_NO_PERMITIDA);
        }
    }
}
