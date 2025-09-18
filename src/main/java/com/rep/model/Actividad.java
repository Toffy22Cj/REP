package com.rep.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Getter
@Setter
@ToString(exclude = {"profesorMateria", "preguntas"})
@Entity
@Slf4j
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Table(name = "actividades")
public class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('examen', 'quiz', 'taller')", nullable = false)
    private TipoActividad tipo;

    private String descripcion;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_materia_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProfesorMateria profesorMateria;

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Pregunta> preguntas = new HashSet<>(); // Cambia List por Set



@CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "permitir_reintentos", nullable = false)
    private Boolean permitirReintentos = false;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "hora_entrega")
    private LocalTime horaEntrega = LocalTime.of(23, 59); // Hora por defecto: 11:59 PM

    public LocalDateTime getFechaHoraEntrega() {
        if (fechaEntrega == null) return null;
        return LocalDateTime.of(fechaEntrega, horaEntrega);
    }
    public Profesor getProfesor() {
        return this.profesorMateria != null ? this.profesorMateria.getProfesor() : null;
    }

    public enum TipoActividad {
        EXAMEN("examen"),
        QUIZ("quiz"),
        TALLER("taller");

        private final String dbValue;

        TipoActividad(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        @JsonCreator
        public static TipoActividad forValue(String value) {
            for (TipoActividad tipo : TipoActividad.values()) {
                if (tipo.dbValue.equalsIgnoreCase(value)) {
                    return tipo;
                }
            }
            throw new IllegalArgumentException("Tipo de actividad no válido: " + value);
        }

        @JsonValue
        public String toValue() {
            return this.dbValue;
        }
    }

    public Materia getMateria() {
        return this.profesorMateria != null ? this.profesorMateria.getMateria() : null;
    }

    public Curso getCurso() {
        return this.profesorMateria != null ? this.profesorMateria.getCurso() : null;
    }

    public void setProfesor(Profesor profesor) {
        if (this.profesorMateria == null) {
            this.profesorMateria = new ProfesorMateria();
        }
        this.profesorMateria.setProfesor(profesor);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Actividad)) return false;
        Actividad actividad = (Actividad) o;
        return id != null && id.equals(actividad.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}