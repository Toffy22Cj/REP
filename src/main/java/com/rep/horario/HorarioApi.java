package com.rep.horario;

import com.rep.horario.HorarioDTOs.Bloque;
import com.rep.horario.HorarioDTOs.GuardarBloque;
import com.rep.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Horario de clases: gestión del administrador ({@code /api/admin/horario}) y consulta
 * del estudiante (su curso) y del profesor (sus clases).
 */
@RestController
public class HorarioApi {
    private final HorarioService horario;

    public HorarioApi(HorarioService horario) {
        this.horario = horario;
    }

    @GetMapping("/api/admin/horario")
    public List<Bloque> listar(@RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long profesorId) {
        return horario.listar(cursoId, profesorId);
    }

    @PostMapping("/api/admin/horario")
    public Bloque crear(@Valid @RequestBody GuardarBloque dto) {
        return horario.crear(dto);
    }

    @PutMapping("/api/admin/horario/{id}")
    public Bloque actualizar(@PathVariable Long id, @Valid @RequestBody GuardarBloque dto) {
        return horario.actualizar(id, dto);
    }

    @DeleteMapping("/api/admin/horario/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        horario.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/estudiante/{id}/horario")
    public List<Bloque> deEstudiante(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null || (!id.equals(usuario.getId()) && usuario.getRol() != Usuario.Rol.ADMIN)) {
            throw new AccessDeniedException("No tiene acceso al horario de este estudiante");
        }
        return horario.deEstudiante(id);
    }

    @GetMapping("/api/profesor/horario")
    public List<Bloque> deProfesor(@AuthenticationPrincipal Usuario usuario) {
        return horario.deProfesor(usuario.getId());
    }
}
