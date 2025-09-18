package com.rep.repositories;

import com.rep.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    Optional<Curso> findByGradoAndGrupo(Integer grado, String grupo);

    // Consulta personalizada: Cursos ordenados por grado
    List<Curso> findAllByOrderByGradoAsc();
    // En CursoRepository.java
    boolean existsByGradoAndGrupo(Integer grado, String grupo);
    // Consulta nativa: Contar estudiantes por curso
    @Query(value = "SELECT c.id, c.grado, c.grupo, COUNT(e.id) as cantidad " +
            "FROM cursos c LEFT JOIN estudiantes e ON c.id = e.curso_id " +
            "GROUP BY c.id", nativeQuery = true)
    List<Object[]> contarEstudiantesPorCurso();
    // Añade este nuevo método
    boolean existsByGradoAndGrupoAndIdNot(Integer grado, String grupo, Long id);

}

