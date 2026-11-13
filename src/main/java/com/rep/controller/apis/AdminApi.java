package com.rep.controller.apis;

import com.rep.dto.auth.ActualizarUsuarioDTO;
import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.model.*;
import com.rep.service.logica.AdminAcademicoService;
import com.rep.service.logica.AdminUsuarioService;
import com.rep.service.logica.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API de administración ({@code ADMIN}). Controlador delgado: la lógica y las reglas
 * están en {@link AdminUsuarioService} y {@link AdminAcademicoService}, y los errores
 * se devuelven como {@code ApiError} con un código del catálogo ({@code docs/errores.md}).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApi {

    /** Cuerpo para crear/actualizar cursos. */
    public record CursoRequest(Integer grado, String grupo) {
    }

    /** Cuerpo para crear/actualizar materias. */
    public record MateriaRequest(String nombre) {
    }

    private final AdminUsuarioService usuarios;
    private final AdminAcademicoService academico;
    private final AuditoriaService auditoria;

    public AdminApi(AdminUsuarioService usuarios, AdminAcademicoService academico, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.academico = academico;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------ cursos

    @GetMapping("/cursos")
    public List<Curso> listarCursos() {
        return academico.listarCursos();
    }

    @GetMapping("/cursos/{id}")
    public Curso obtenerCurso(@PathVariable Long id) {
        return academico.obtenerCurso(id);
    }

    @PostMapping("/cursos")
    public Curso crearCurso(@RequestBody CursoRequest request) {
        return academico.crearCurso(request.grado(), request.grupo());
    }

    @PutMapping("/cursos/{id}")
    public Curso actualizarCurso(@PathVariable Long id, @RequestBody CursoRequest request) {
        return academico.actualizarCurso(id, request.grado(), request.grupo());
    }

    @DeleteMapping("/cursos/{id}")
    public ResponseEntity<Void> eliminarCurso(@PathVariable Long id) {
        academico.eliminarCurso(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cursos/{id}/estudiantes")
    public List<Estudiante> estudiantesDeCurso(@PathVariable Long id) {
        return academico.estudiantesDeCurso(id);
    }

    // ------------------------------------------------------------------ materias

    @GetMapping("/materias")
    public List<Materia> listarMaterias() {
        return academico.listarMaterias();
    }

    @GetMapping("/materias/{id}")
    public Materia obtenerMateria(@PathVariable Long id) {
        return academico.obtenerMateria(id);
    }

    @PostMapping("/materias")
    public Materia crearMateria(@RequestBody MateriaRequest request) {
        return academico.crearMateria(request.nombre());
    }

    @PutMapping("/materias/{id}")
    public Materia actualizarMateria(@PathVariable Long id, @RequestBody MateriaRequest request) {
        return academico.actualizarMateria(id, request.nombre());
    }

    @DeleteMapping("/materias/{id}")
    public ResponseEntity<Void> eliminarMateria(@PathVariable Long id) {
        academico.eliminarMateria(id);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------ asignaciones

    @GetMapping("/asignaciones")
    public List<ProfesorMateria> listarAsignaciones(@RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long materiaId) {
        return academico.listarAsignaciones(cursoId, materiaId);
    }

    @GetMapping("/asignaciones/curso-materia")
    public List<ProfesorMateria> asignacionesPorCursoYMateria(@RequestParam Long cursoId, @RequestParam Long materiaId) {
        return academico.listarAsignaciones(cursoId, materiaId);
    }

    @PostMapping("/asignaciones")
    public ProfesorMateria crearAsignacion(@RequestBody ProfesorMateriaRequest request) {
        return academico.crearAsignacion(request);
    }

    @DeleteMapping("/asignaciones/{id}")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable Long id) {
        academico.eliminarAsignacion(id);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------ usuarios

    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios(@RequestParam(required = false) String rol) {
        return usuarios.listar(rol);
    }

    @PostMapping("/usuarios")
    public Usuario crearUsuario(@Valid @RequestBody RegistroUsuarioDTO dto) {
        return usuarios.crear(dto);
    }

    @GetMapping("/usuarios/{id}")
    public Map<String, Object> obtenerUsuario(@PathVariable Long id) {
        Usuario usuario = usuarios.obtener(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", usuario.getId());
        response.put("nombre", usuario.getNombre());
        response.put("apellido", usuario.getApellido());
        response.put("correo", usuario.getCorreo());
        response.put("rol", usuario.getRol());
        return response;
    }

    @PutMapping("/usuarios/{id}")
    public Usuario actualizarUsuario(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioDTO dto) {
        return usuarios.actualizar(id, dto);
    }

    @PutMapping("/usuarios/{id}/estado")
    public Usuario cambiarEstadoUsuario(@PathVariable Long id, @RequestParam boolean activo) {
        return usuarios.cambiarEstado(id, activo);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarios.eliminar(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/estudiantes/{id}/curso")
    public Estudiante asignarCursoAEstudiante(@PathVariable Long id, @RequestParam Long cursoId) {
        return usuarios.asignarCurso(id, cursoId);
    }

    // ------------------------------------------------------------------ profesores

    @GetMapping("/profesores/{id}")
    public Profesor obtenerProfesor(@PathVariable Long id) {
        return usuarios.obtenerProfesor(id);
    }

    @PutMapping("/profesores/{id}/estado")
    public Profesor actualizarEstadoProfesor(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return usuarios.cambiarEstadoProfesor(id, request.get("estado"));
    }

    @GetMapping("/profesores/{id}/asignaciones")
    public List<ProfesorMateria> asignacionesDeProfesor(@PathVariable Long id) {
        return academico.asignacionesDeProfesor(id);
    }

    @GetMapping("/profesores/{id}/materias")
    public List<Materia> materiasDeProfesor(@PathVariable Long id) {
        return academico.asignacionesDeProfesor(id).stream().map(ProfesorMateria::getMateria).distinct().toList();
    }

    // ------------------------------------------------------------------ auditoría y estadísticas

    @GetMapping("/stats")
    public Map<String, Long> estadisticas() {
        return academico.estadisticas();
    }

    @GetMapping("/auditoria")
    public List<AuditLog> auditoria(@RequestParam(defaultValue = "500") int limite) {
        return auditoria.listar(limite);
    }
}
