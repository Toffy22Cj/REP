package com.rep.controller.apis;

import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.model.*;
import com.rep.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost")
@RestController
@RequestMapping("/api/admin")
public class AdminApi {
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminApi(UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            ProfesorRepository profesorRepository,
            CursoRepository cursoRepository,
            MateriaRepository materiaRepository,
            ProfesorMateriaRepository profesorMateriaRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------- Gestión de Cursos --------------------
    @GetMapping("/cursos")
    public ResponseEntity<List<Curso>> listarCursos() {
        try {
            return ResponseEntity.ok(cursoRepository.findAllByOrderByGradoAsc());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cursos/{id}")
    public ResponseEntity<?> obtenerCursoPorId(@PathVariable Long id) {
        try {
            return cursoRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cursos")
    public ResponseEntity<?> registrarCurso(@RequestBody Curso curso) {
        if (cursoRepository.findByGradoAndGrupo(curso.getGrado(), curso.getGrupo()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Ya existe un curso con este grado y grupo");
        }

        try {
            Curso nuevoCurso = new Curso();
            nuevoCurso.setGrado(curso.getGrado());
            nuevoCurso.setGrupo(curso.getGrupo());
            Curso guardado = cursoRepository.save(nuevoCurso);
            registrarAuditoria("CREAR_CURSO", "ID: " + guardado.getId() + ", " + guardado.getNombre());
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al registrar el curso: " + e.getMessage());
        }
    }

    @PutMapping("/cursos/{id}")
    public ResponseEntity<?> actualizarCurso(@PathVariable Long id, @RequestBody Curso cursoActualizado) {
        try {
            return cursoRepository.findById(id)
                    .map(curso -> {
                        if (cursoRepository.existsByGradoAndGrupoAndIdNot(
                                cursoActualizado.getGrado(),
                                cursoActualizado.getGrupo(),
                                id)) {
                            return ResponseEntity.badRequest()
                                    .body("Ya existe otro curso con este grado y grupo");
                        }
                        curso.setGrado(cursoActualizado.getGrado());
                        curso.setGrupo(cursoActualizado.getGrupo());
                        Curso guardado = cursoRepository.save(curso);
                        registrarAuditoria("ACTUALIZAR_CURSO", "ID: " + guardado.getId() + ", " + guardado.getNombre());
                        return ResponseEntity.ok(guardado);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al actualizar curso: " + e.getMessage());
        }
    }

    @DeleteMapping("/cursos/{id}")
    public ResponseEntity<?> eliminarCurso(@PathVariable Long id) {
        try {
            Curso curso = cursoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

            if (!estudianteRepository.findByCursoId(id).isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No se puede eliminar el curso porque tiene estudiantes asignados");
            }

            if (!profesorMateriaRepository.findByCursoId(id).isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No se puede eliminar el curso porque tiene materias asignadas");
            }

            registrarAuditoria("ELIMINAR_CURSO", "ID: " + id + ", " + curso.getNombre());
            cursoRepository.delete(curso);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar curso: " + e.getMessage());
        }
    }

    @GetMapping("/cursos/{id}/estudiantes")
    public ResponseEntity<List<Estudiante>> getEstudiantesPorCurso(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudianteRepository.findByCursoId(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/asignaciones")
    public ResponseEntity<List<ProfesorMateria>> getAsignaciones(
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long materiaId) {
        try {
            if (cursoId != null && materiaId != null) {
                return ResponseEntity.ok(profesorMateriaRepository
                        .findByCursoIdAndMateriaId(cursoId, materiaId));
            } else if (cursoId != null) {
                return ResponseEntity.ok(profesorMateriaRepository.findByCursoId(cursoId));
            } else if (materiaId != null) {
                return ResponseEntity.ok(profesorMateriaRepository.findByMateriaId(materiaId));
            }
            return ResponseEntity.ok(profesorMateriaRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/asignaciones/curso-materia")
    public ResponseEntity<List<ProfesorMateria>> getAsignacionesPorCursoYMateria(
            @RequestParam Long cursoId,
            @RequestParam Long materiaId) {
        try {
            List<ProfesorMateria> asignaciones = profesorMateriaRepository
                    .findByCursoIdAndMateriaId(cursoId, materiaId);
            return ResponseEntity.ok(asignaciones);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------- Gestión de Materias --------------------
    @GetMapping("/materias")
    public ResponseEntity<List<Materia>> listarMaterias() {
        try {
            return ResponseEntity.ok(materiaRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/materias/{id}")
    public ResponseEntity<?> obtenerMateriaPorId(@PathVariable Long id) {
        try {
            return materiaRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/materias")
    public ResponseEntity<?> crearMateria(@RequestBody Materia materia) {
        try {
            if (materiaRepository.findByNombre(materia.getNombre()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: Ya existe una materia con este nombre");
            }
            Materia guardada = materiaRepository.save(materia);
            registrarAuditoria("CREAR_MATERIA", "ID: " + guardada.getId() + ", " + guardada.getNombre());
            return ResponseEntity.ok(guardada);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al crear materia: " + e.getMessage());
        }
    }

    @DeleteMapping("/materias/{id}")
    public ResponseEntity<?> eliminarMateria(@PathVariable Long id) {
        try {
            if (!materiaRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            if (profesorMateriaRepository.existsByMateriaId(id)) {
                return ResponseEntity.badRequest()
                        .body("No se puede eliminar la materia porque está asignada a uno o más profesores");
            }

            registrarAuditoria("ELIMINAR_MATERIA", "ID: " + id);
            materiaRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar materia: " + e.getMessage());
        }
    }

    // -------------------- Gestión de Usuarios --------------------
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarUsuariosPorRol(@RequestParam(required = false) String rol) {
        try {
            List<Usuario> usuarios;
            if (rol != null) {
                usuarios = usuarioRepository.findByRol(Usuario.Rol.valueOf(rol));
            } else {
                usuarios = usuarioRepository.findAll();
            }
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        try {
            if (usuarioRepository.findByIdentificacion(usuario.getIdentificacion()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: La identificación ya existe");
            }
            if (usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: El correo ya existe");
            }

            // Validación de Edad vs Tipo Identificación
            if (usuario.getEdad() != null && usuario.getTipoIdentificacion() != null) {
                String idStr = usuario.getIdentificacion();
                if (usuario.getTipoIdentificacion() == Usuario.TipoIdentificacion.CC) {
                    if (usuario.getEdad() < 18) {
                        return ResponseEntity.badRequest()
                                .body("Error: No se permite Cédula (CC) para menores de edad (menor a 18 años)");
                    }
                    if (idStr.length() < 6 || idStr.length() > 10) {
                        return ResponseEntity.badRequest()
                                .body("Error: La Cédula (CC) debe tener entre 6 y 10 dígitos");
                    }
                }
                if (usuario.getTipoIdentificacion() == Usuario.TipoIdentificacion.TI) {
                    if (usuario.getEdad() >= 18) {
                        return ResponseEntity.badRequest().body(
                                "Error: No se permite Tarjeta Identidad (TI) para mayores de edad (18 años o más)");
                    }
                    if (idStr.length() != 10) {
                        return ResponseEntity.badRequest()
                                .body("Error: La Tarjeta de Identidad (TI) debe tener exactamente 10 dígitos");
                    }
                }
            }

            usuario.setContraseña(passwordEncoder.encode(usuario.getContraseña()));
            Usuario guardado = usuarioRepository.save(usuario);

            registrarAuditoria("CREAR_USUARIO", "ID: " + guardado.getId() + ", ROL: " + guardado.getRol());

            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al registrar usuario: " + e.getMessage());
        }
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long id) {
        try {
            return usuarioRepository.findById(id)
                    .map(usuario -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", usuario.getId());
                        response.put("nombre", usuario.getNombre());
                        response.put("apellido", usuario.getApellido());
                        response.put("correo", usuario.getCorreo());
                        response.put("rol", usuario.getRol());
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener usuario: " + e.getMessage());
        }
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioActualizado) {
        try {
            return usuarioRepository.findById(id)
                    .map(usuario -> {
                        usuario.setNombre(usuarioActualizado.getNombre());
                        usuario.setApellido(usuarioActualizado.getApellido());
                        usuario.setCorreo(usuarioActualizado.getCorreo());
                        // Only update password if provided and not empty
                        if (usuarioActualizado.getContraseña() != null
                                && !usuarioActualizado.getContraseña().isEmpty()) {
                            usuario.setContraseña(passwordEncoder.encode(usuarioActualizado.getContraseña()));
                        }
                        usuario.setRol(usuarioActualizado.getRol());
                        usuario.setActivo(usuarioActualizado.isActivo());
                        Usuario guardado = usuarioRepository.save(usuario);
                        registrarAuditoria("ACTUALIZAR_USUARIO",
                                "ID: " + id + ", Nombre: " + guardado.getNombre() + " " + guardado.getApellido());
                        return ResponseEntity.ok(guardado);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al actualizar usuario: " + e.getMessage());
        }
    }

    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<?> cambiarEstadoUsuario(@PathVariable Long id, @RequestParam boolean activo) {
        try {
            return usuarioRepository.findById(id)
                    .map(usuario -> {
                        usuario.setActivo(activo);
                        Usuario guardado = usuarioRepository.save(usuario);
                        registrarAuditoria("CAMBIAR_ESTADO_USUARIO", "ID: " + id + ", Activo: " + activo);
                        return ResponseEntity.ok(guardado);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al cambiar estado: " + e.getMessage());
        }
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (usuario.getRol() == Usuario.Rol.PROFESOR &&
                    !profesorMateriaRepository.findByProfesorId(id).isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No se puede eliminar el profesor porque tiene materias asignadas");
            }

            registrarAuditoria("ELIMINAR_USUARIO", "ID: " + id + ", Rol: " + usuario.getRol());
            usuarioRepository.delete(usuario);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar usuario: " + e.getMessage());
        }
    }

    // -------------------- Gestión de Estudiantes --------------------
    @PutMapping("/estudiantes/{id}/curso")
    public ResponseEntity<?> asignarCursoAEstudiante(@PathVariable Long id, @RequestParam Long cursoId) {
        try {
            Estudiante estudiante = estudianteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

            Curso curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

            if (estudiante.getCurso() != null && estudiante.getCurso().getId().equals(cursoId)) {
                return ResponseEntity.badRequest().body("El estudiante ya está en este curso");
            }

            estudiante.setCurso(curso);
            return ResponseEntity.ok(estudianteRepository.save(estudiante));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al asignar curso: " + e.getMessage());
        }
    }

    // -------------------- Gestión de Profesores --------------------
    @GetMapping("/profesores/{id}")
    public ResponseEntity<?> obtenerProfesorPorId(@PathVariable Long id) {
        try {
            return profesorRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/profesores/{id}/estado")
    public ResponseEntity<?> actualizarEstadoProfesor(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Profesor profesor = profesorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Profesor no encontrado"));

            String estadoStr = request.get("estado").toLowerCase();
            if (estadoStr == null) {
                return ResponseEntity.badRequest().body("El campo 'estado' es requerido");
            }

            profesor.setEstado(Profesor.EstadoProfesor.valueOf(estadoStr));
            profesorRepository.save(profesor);
            return ResponseEntity.ok(profesor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Estado no válido. Use 'activo' o 'retirado'");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al actualizar estado: " + e.getMessage());
        }
    }

    // -------------------- Gestión de Asignaciones Profesor-Materia
    // --------------------
    @GetMapping("/profesores/{id}/asignaciones")
    public ResponseEntity<List<ProfesorMateria>> getAsignacionesPorProfesor(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(profesorMateriaRepository.findByProfesorId(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/profesores/{id}/materias")
    public ResponseEntity<List<Materia>> getMateriasPorProfesor(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    profesorMateriaRepository.findByProfesorId(id)
                            .stream()
                            .map(ProfesorMateria::getMateria)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/asignaciones")
    public ResponseEntity<?> crearAsignacion(@RequestBody ProfesorMateriaRequest request) {
        try {
            Profesor profesor = profesorRepository.findById(request.getProfesorId())
                    .orElseThrow(() -> new RuntimeException("Profesor no encontrado"));
            Materia materia = materiaRepository.findById(request.getMateriaId())
                    .orElseThrow(() -> new RuntimeException("Materia no encontrada"));
            Curso curso = cursoRepository.findById(request.getCursoId())
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

            if (profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(
                    profesor.getId(),
                    materia.getId(),
                    curso.getId())) {
                return ResponseEntity.badRequest().body("Esta asignación ya existe");
            }

            ProfesorMateria asignacion = new ProfesorMateria();
            asignacion.setProfesor(profesor);
            asignacion.setMateria(materia);
            asignacion.setCurso(curso);

            return ResponseEntity.ok(profesorMateriaRepository.save(asignacion));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al crear asignación: " + e.getMessage());
        }
    }

    @DeleteMapping("/asignaciones/{id}")
    public ResponseEntity<?> eliminarAsignacion(@PathVariable Long id) {
        try {
            profesorMateriaRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar asignación: " + e.getMessage());
        }
    }

    // -------------------- Auditoría y Estadísticas --------------------
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("usuarios", usuarioRepository.count());
        stats.put("estudiantes", estudianteRepository.count());
        stats.put("profesores", profesorRepository.count());
        stats.put("cursos", cursoRepository.count());
        stats.put("materias", materiaRepository.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<AuditLog>> listarAuditoria() {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByTimestampDesc());
    }

    private void registrarAuditoria(String action, String details) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(adminUsername, action, details));
    }
}