package com.rep.repositories;

import com.rep.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    List<Recurso> findByProfesorMateriaCursoId(Long cursoId);

    /** Filtro combinado; cualquier parámetro nulo no filtra. Más recientes primero. */
    @Query("SELECT r FROM Recurso r JOIN FETCH r.profesorMateria pm JOIN FETCH pm.materia JOIN FETCH pm.curso "
            + "JOIN FETCH pm.profesor WHERE (:cursoId IS NULL OR pm.curso.id = :cursoId) "
            + "AND (:materiaId IS NULL OR pm.materia.id = :materiaId) "
            + "AND (:profesorId IS NULL OR pm.profesor.id = :profesorId) "
            + "ORDER BY r.fechaSubida DESC")
    List<Recurso> buscar(@Param("cursoId") Long cursoId, @Param("materiaId") Long materiaId,
            @Param("profesorId") Long profesorId);
}
