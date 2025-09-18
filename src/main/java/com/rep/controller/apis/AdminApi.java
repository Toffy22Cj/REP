package com.rep.controller.apis;

import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.model.*;
import com.rep.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public AdminApi(UsuarioRepository usuarioRepository,
                    EstudianteRepository estudianteRepository,
                    ProfesorRepository profesorRepository,
                    CursoRepository cursoRepository,
                    MateriaRepository materiaRepository,
                    ProfesorMateriaRepository profesorMateriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
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
            return ResponseEntity.ok(cursoRepository.save(nuevoCurso));
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
                        return ResponseEntity.ok(cursoRepository.save(curso));
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
                return ResponseEntity.badRequest().body("No se puede eliminar el curso porque tiene estudiantes asignados");
            }

            if (!profesorMateriaRepository.findByCursoId(id).isEmpty()) {
                return ResponseEntity.badRequest().body("No se puede eliminar el curso porque tiene materias asignadas");
            }

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
            return ResponseEntity.ok(materiaRepository.save(materia));
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
                return ResponseEntity.badRequest().body("No se puede eliminar la materia porque está asignada a uno o más profesores");
            }

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

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long id) {
        try {
            return usuarioRepository.findById(id)
                    .map(usuario -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", usuario.getId());
                        response.put("nombre", usuario.getNombre());
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
                        usuario.setCorreo(usuarioActualizado.getCorreo());
                        usuario.setRol(usuarioActualizado.getRol());
                        usuario.setActivo(usuarioActualizado.isActivo());
                        return ResponseEntity.ok(usuarioRepository.save(usuario));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al actualizar usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (usuario.getRol() == Usuario.Rol.PROFESOR &&
                    !profesorMateriaRepository.findByProfesorId(id).isEmpty()) {
                return ResponseEntity.badRequest().body("No se puede eliminar el profesor porque tiene materias asignadas");
            }

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

    // -------------------- Gestión de Asignaciones Profesor-Materia --------------------
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
}