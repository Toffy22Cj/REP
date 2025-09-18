package com.rep.repositories;

import com.rep.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    List<Estudiante> findByCursoId(Long cursoId);
    List<Estudiante> findByEstado(Estudiante.EstadoEstudiante estado);

    // Añade estos métodos:
    List<Estudiante> findByCurso_GradoAndCurso_Grupo(Integer grado, String grupo);
    @Modifying
    @Query(value = "INSERT INTO estudiantes (id, edad, estado, curso_id) VALUES (:id, :edad, :estado, :cursoId)", nativeQuery = true)
    void insertEstudiante(@Param("id") Long id,
                          @Param("edad") Integer edad,
                          @Param("estado") String estado,
                          @Param("cursoId") Long cursoId);
    @Query("SELECT e FROM Estudiante e WHERE e.edad BETWEEN :minEdad AND :maxEdad")
    List<Estudiante> findByEdadBetween(@Param("minEdad") int minEdad, @Param("maxEdad") int maxEdad);
}