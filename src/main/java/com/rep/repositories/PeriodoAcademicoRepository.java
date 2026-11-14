package com.rep.repositories;

import com.rep.model.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {
    List<PeriodoAcademico> findByAnioOrderByNumeroAsc(Integer anio);

    List<PeriodoAcademico> findAllByOrderByAnioDescNumeroAsc();

    @Query("SELECT MAX(p.anio) FROM PeriodoAcademico p")
    Optional<Integer> ultimoAnio();
}
