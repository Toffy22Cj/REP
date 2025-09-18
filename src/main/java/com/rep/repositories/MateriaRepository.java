package com.rep.repositories;

import com.rep.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {
    Optional<Materia> findByNombre(String nombre);

    // Consulta corregida usando ProfesorMateria
    @Query("SELECT DISTINCT pm.materia FROM ProfesorMateria pm WHERE pm.profesor.id = :profesorId")
    List<Materia> findByProfesorId(@Param("profesorId") Long profesorId);

    // Consulta corregida para verificar acceso
    @Query("SELECT COUNT(pm) > 0 FROM ProfesorMateria pm WHERE pm.profesor.id = :profesorId AND pm.materia.id = :materiaId")
    boolean existsByProfesorIdAndMateriaId(@Param("profesorId") Long profesorId, @Param("materiaId") Long materiaId);

    // Consulta para búsqueda por nombre
    List<Materia> findByNombreContainingIgnoreCase(String nombre);
    @Query("SELECT DISTINCT pm.materia FROM ProfesorMateria pm WHERE pm.curso.id = :cursoId")
    List<Materia> findByCursoId(@Param("cursoId") Long cursoId);
}