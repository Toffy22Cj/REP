package com.rep.repositories;

import com.rep.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

        @Query("SELECT a FROM Asistencia a JOIN FETCH a.estudiante JOIN FETCH a.profesorMateria WHERE a.profesorMateria.id = :profesorMateriaId AND a.fecha = :fecha")
        List<Asistencia> findByProfesorMateriaAndFecha(@Param("profesorMateriaId") Long profesorMateriaId,
                        @Param("fecha") LocalDate fecha);

        Optional<Asistencia> findByProfesorMateriaIdAndEstudianteIdAndFecha(Long profesorMateriaId, Long estudianteId,
                        LocalDate fecha);

        @Query("SELECT DISTINCT a.fecha FROM Asistencia a WHERE a.profesorMateria.id = :profesorMateriaId ORDER BY a.fecha DESC")
        List<LocalDate> findFechasByProfesorMateriaId(@Param("profesorMateriaId") Long profesorMateriaId);
}
