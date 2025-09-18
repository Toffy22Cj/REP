package com.rep.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true, exclude = {"curso"}) // Excluir el campo curso
@Data
@Entity
@Table(name = "estudiantes")
@PrimaryKeyJoinColumn(name = "id")
public class Estudiante extends Usuario {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    @JsonBackReference
    private Curso curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('activo', 'retirado', 'graduado', 'suspendido')")
    private EstadoEstudiante estado = EstadoEstudiante.activo;

    @Column(name = "edad", nullable = false)
    private Integer edadEstudiante;

    public enum EstadoEstudiante {
        activo("Activo"),
        retirado("Retirado"),
        graduado("Graduado"),
        suspendido("Suspendido");

        private final String descripcion;

        EstadoEstudiante(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    @PostLoad
    private void postLoad() {
        if(this.edadEstudiante != null) {
            super.setEdad(this.edadEstudiante);
        }
    }

    @PrePersist
    @PreUpdate
    protected void prePersistEstudiante() {
        if (this.getCreadoEn() == null) {
            this.setCreadoEn(LocalDateTime.now());
        }
        if (this.getActivo() == null) {
            this.setActivo(true);
        }

        if(super.getEdad() != null) {
            this.edadEstudiante = super.getEdad();
        }
    }
}