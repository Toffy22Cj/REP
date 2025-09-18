package com.rep.repositories;

import com.rep.model.Materia;
import com.rep.model.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    // Consulta personalizada: Buscar profesores por estado
    List<Profesor> findByEstado(Profesor.EstadoProfesor estado);

    // Consulta con JOIN: Materias que enseña un profesor
    @Query("SELECT pm.materia FROM ProfesorMateria pm WHERE pm.profesor.id = :profesorId")
    List<Materia> findMateriasByProfesorId(@Param("profesorId") Long profesorId);
}