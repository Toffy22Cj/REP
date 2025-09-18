package com.rep.service.logica;

import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.model.*;
import com.rep.repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

@Service
public class UsuarioRegistrationService {
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursoRepository cursoRepository;

    public UsuarioRegistrationService(UsuarioRepository usuarioRepository,
                                      EstudianteRepository estudianteRepository,
                                      ProfesorRepository profesorRepository,
                                      PasswordEncoder passwordEncoder,
                                      CursoRepository cursoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.passwordEncoder = passwordEncoder;
        this.cursoRepository = cursoRepository;
    }

    public ResponseEntity<?> registrarUsuario(RegistroUsuarioDTO usuarioDTO) {
        try {
            // Validaciones básicas
            if (usuarioRepository.existsByCorreo(usuarioDTO.getCorreo())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("El correo electrónico ya está registrado");
            }

            if (usuarioRepository.existsByIdentificacion(usuarioDTO.getIdentificacion())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("La identificación ya está registrada");
            }

            // Validaciones específicas por rol
            if(usuarioDTO.getRol().equals("ESTUDIANTE")) {
                if(usuarioDTO.getEdad() == null) {
                    return ResponseEntity.badRequest()
                            .body("La edad es obligatoria para estudiantes");
                }
                if(usuarioDTO.getCursoId() == null) {
                    return ResponseEntity.badRequest()
                            .body("El curso es obligatorio para estudiantes");
                }
            }

            Usuario usuario = crearUsuarioSegunRol(usuarioDTO);
            Usuario usuarioGuardado = usuarioRepository.save(usuario);

            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioGuardado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error en los datos: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error interno: " + e.getMessage());
        }
    }

    private Usuario crearUsuarioSegunRol(RegistroUsuarioDTO dto) {
        Usuario usuario;
        switch(Usuario.Rol.valueOf(dto.getRol())) {
            case ESTUDIANTE:
                Estudiante estudiante = new Estudiante();
                copiarPropiedadesComunes(estudiante, dto);
                estudiante.setEdad(dto.getEdad());
                estudiante.setCurso(cursoRepository.findById(dto.getCursoId())
                        .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado")));
                estudiante.setEstado(Estudiante.EstadoEstudiante.activo);
                return estudiante;

            case PROFESOR:
                Profesor profesor = new Profesor();
                copiarPropiedadesComunes(profesor, dto);
                profesor.setEstado(Profesor.EstadoProfesor.activo);
                profesor.setFechaIngreso(dto.getFechaIngreso());
                return profesor;

            default:
                usuario = new Usuario();
                copiarPropiedadesComunes(usuario, dto);
                return usuario;
        }
    }

    private void copiarPropiedadesComunes(Usuario usuario, RegistroUsuarioDTO dto) {
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());
        usuario.setIdentificacion(dto.getIdentificacion());
        usuario.setTipoIdentificacion(Usuario.TipoIdentificacion.valueOf(dto.getTipoIdentificacion()));
        usuario.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        usuario.setRol(Usuario.Rol.valueOf(dto.getRol()));
        usuario.setActivo(dto.isActivo());
        usuario.setFechaIngreso(dto.getFechaIngreso());

        if(dto.getEdad() != null) {
            usuario.setEdad(dto.getEdad());
        }
    }
}