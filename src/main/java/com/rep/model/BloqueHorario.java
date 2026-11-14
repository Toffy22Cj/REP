package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Bloque del horario de clases: una asignación (profesor · materia · curso) en un día y
 * franja horaria, con aula opcional. Lo administra el administrador.
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "bloques_horario", indexes = {
        @Index(name = "idx_bloque_asignacion", columnList = "profesor_materia_id"),
        @Index(name = "idx_bloque_dia", columnList = "dia") })
public class BloqueHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_materia_id", nullable = false)
    private ProfesorMateria profesorMateria;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // VARCHAR y no ENUM nativo: añadir valores no rompe MySQL
    @Column(nullable = false, length = 10)
    private DayOfWeek dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(length = 40)
    private String aula;

    /** Se cruza con otro bloque del mismo día (los extremos pueden tocarse: 8:00–9:00 y 9:00–10:00). */
    public boolean seCruzaCon(BloqueHorario otro) {
        return dia == otro.dia && horaInicio.isBefore(otro.horaFin) && otro.horaInicio.isBefore(horaFin);
    }
}
