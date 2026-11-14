package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Periodo académico de un año lectivo (p. ej. 4 periodos de 25 %), según el sistema
 * institucional de evaluación del Decreto 1290 de 2009. Una actividad pertenece al
 * periodo cuya fecha de inicio y fin contienen su fecha de entrega.
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "periodos_academicos",
        uniqueConstraints = @UniqueConstraint(columnNames = { "anio", "numero" }),
        indexes = @Index(name = "idx_periodo_fechas", columnList = "fecha_inicio, fecha_fin"))
public class PeriodoAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Año lectivo (calendario A: el año de inicio). */
    @Column(nullable = false)
    private Integer anio;

    /** Orden dentro del año: 1, 2, 3… */
    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /** Peso en la nota anual (1–100). La suma de un año no puede pasar de 100. */
    @Column(nullable = false)
    private Integer porcentaje;

    public boolean contiene(LocalDate fecha) {
        return fecha != null && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }
}
