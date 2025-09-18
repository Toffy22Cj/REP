package com.rep.controller.apis;

import com.rep.model.Curso;
import com.rep.repositories.CursoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost")
@RestController
@RequestMapping("/api/public/cursos")
public class PublicCursoApi {
    private final CursoRepository cursoRepository;

    public PublicCursoApi(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Curso>> listarCursosDisponibles() {
        try {
            return ResponseEntity.ok(cursoRepository.findAllByOrderByGradoAsc());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}