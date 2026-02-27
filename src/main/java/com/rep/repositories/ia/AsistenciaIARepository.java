package com.rep.repositories.ia;

import com.rep.model.ia.AsistenciaIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaIARepository extends JpaRepository<AsistenciaIA, Long> {
    List<AsistenciaIA> findByEstudianteId(Long estudianteId);

    List<AsistenciaIA> findByMateriaId(Long materiaId);

    List<AsistenciaIA> findByEstudianteIdAndMateriaId(Long estudianteId, Long materiaId);

    Optional<AsistenciaIA> findByEstudianteIdAndMateriaIdAndFecha(Long estudianteId, Long materiaId, LocalDate fecha);
}
