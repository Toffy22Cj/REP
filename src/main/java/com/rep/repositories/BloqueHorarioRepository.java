package com.rep.repositories;

import com.rep.model.BloqueHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.util.List;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, Long> {
    List<BloqueHorario> findByProfesorMateriaCursoId(Long cursoId);

    List<BloqueHorario> findByProfesorMateriaProfesorId(Long profesorId);

    @Query("SELECT b FROM BloqueHorario b WHERE b.dia = :dia AND "
            + "(b.profesorMateria.curso.id = :cursoId OR b.profesorMateria.profesor.id = :profesorId)")
    List<BloqueHorario> candidatosACruce(DayOfWeek dia, Long cursoId, Long profesorId);

    @Modifying
    @Query("DELETE FROM BloqueHorario b WHERE b.profesorMateria.id = :asignacionId")
    int deleteByAsignacion(Long asignacionId);
}
