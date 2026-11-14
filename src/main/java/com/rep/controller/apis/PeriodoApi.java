package com.rep.controller.apis;

import com.rep.dto.notas.PeriodoDTO;
import com.rep.model.PeriodoAcademico;
import com.rep.service.logica.PeriodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Periodos académicos. Consulta para todos los roles ({@code /api/periodos}); alta,
 * edición y borrado solo para el administrador ({@code /api/admin/periodos}).
 */
@RestController
public class PeriodoApi {
    private final PeriodoService periodos;

    public PeriodoApi(PeriodoService periodos) {
        this.periodos = periodos;
    }

    /** Periodos de un año, o del año lectivo vigente si no se indica. */
    @GetMapping({ "/api/periodos", "/api/admin/periodos" })
    public List<PeriodoAcademico> listar(@RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "false") boolean todos) {
        if (todos) {
            return periodos.listar(null);
        }
        return anio != null ? periodos.listar(anio) : periodos.delAnioVigente(LocalDate.now());
    }

    @PostMapping("/api/admin/periodos")
    public PeriodoAcademico crear(@Valid @RequestBody PeriodoDTO dto) {
        return periodos.crear(dto);
    }

    @PutMapping("/api/admin/periodos/{id}")
    public PeriodoAcademico actualizar(@PathVariable Long id, @Valid @RequestBody PeriodoDTO dto) {
        return periodos.actualizar(id, dto);
    }

    @DeleteMapping("/api/admin/periodos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        periodos.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
