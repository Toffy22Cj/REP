package com.rep.controller.apis;

import com.rep.dto.material.MaterialDTO;
import com.rep.model.Usuario;
import com.rep.service.logica.MaterialEstudioService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Material de estudio.
 *
 * <pre>
 * GET    /api/materiales?cursoId=&amp;materiaId=   lista (según el rol)
 * POST   /api/materiales                          multipart: file, titulo, descripcion, materiaId, cursoId (PROFESOR)
 * GET    /api/materiales/{id}/archivo             descarga / previsualización
 * DELETE /api/materiales/{id}                     profesor que lo publicó o ADMIN
 * </pre>
 */
@RestController
@RequestMapping("/api/materiales")
public class MaterialApi {
    private final MaterialEstudioService materialService;

    public MaterialApi(MaterialEstudioService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialDTO> listar(@RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {
        return materialService.listar(usuario, cursoId, materiaId);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<MaterialDTO> publicar(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam Long materiaId,
            @RequestParam Long cursoId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.publicar(usuario, materiaId, cursoId, titulo, descripcion, file));
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return materialService.descargar(usuario, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        materialService.eliminar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
