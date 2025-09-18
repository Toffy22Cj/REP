package com.rep.repositories;

import com.rep.model.ProfesorMateria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorMateriaRepository extends JpaRepository<ProfesorMateria, Long> {
    // Métodos derivados del nombre
    boolean existsByCursoId(Long cursoId);
    long countDistinctCursoByProfesorId(Long profesorId);
    List<ProfesorMateria> findByProfesorId(Long profesorId);
    List<ProfesorMateria> findByMateriaId(Long materiaId);
    List<ProfesorMateria> findByCursoId(Long cursoId);
    boolean existsByMateriaId(Long materiaId);
    boolean existsByProfesorIdAndCursoId(Long profesorId, Long cursoId);

    // Método con consulta JPQL personalizada para verificar existencia
    @Query("SELECT COUNT(pm) > 0 FROM ProfesorMateria pm " +
            "WHERE pm.profesor.id = :profesorId " +
            "AND pm.materia.id = :materiaId " +
            "AND pm.curso.id = :cursoId")
    boolean existsByProfesorIdAndMateriaIdAndCursoId(
            @Param("profesorId") Long profesorId,
            @Param("materiaId") Long materiaId,
            @Param("cursoId") Long cursoId);

    // Nuevo método para obtener la relación completa
    @Query("SELECT pm FROM ProfesorMateria pm " +
            "WHERE pm.profesor.id = :profesorId " +
            "AND pm.materia.id = :materiaId " +
            "AND pm.curso.id = :cursoId")
    Optional<ProfesorMateria> findByProfesorIdAndMateriaIdAndCursoId(
            @Param("profesorId") Long profesorId,
            @Param("materiaId") Long materiaId,
            @Param("cursoId") Long cursoId);
    @Modifying
    @Query("DELETE FROM ProfesorMateria pm WHERE " +
            "pm.profesor.id = :profesorId AND " +
            "pm.materia.id = :materiaId AND " +
            "pm.curso.id = :cursoId")
    void deleteByProfesorIdAndMateriaIdAndCursoId(
            @Param("profesorId") Long profesorId,
            @Param("materiaId") Long materiaId,
            @Param("cursoId") Long cursoId);

    @Query("SELECT pm FROM ProfesorMateria pm " +
            "WHERE pm.curso.id = :cursoId " +
            "AND pm.materia.id = :materiaId")
    List<ProfesorMateria> findByCursoIdAndMateriaId(
            @Param("cursoId") Long cursoId,
            @Param("materiaId") Long materiaId);

}