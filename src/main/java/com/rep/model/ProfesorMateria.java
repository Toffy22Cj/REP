package com.rep.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Objects;

@Data
@Entity
@Table(name = "profesor_materia",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"profesor_id", "materia_id", "curso_id"}))
public class ProfesorMateria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "profesor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_profesor"))
    private Profesor profesor;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "materia_id", nullable = false, foreignKey = @ForeignKey(name = "fk_materia"))
    private Materia materia;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false, foreignKey = @ForeignKey(name = "fk_curso"))
    private Curso curso;
    // Método helper para evitar duplicados
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfesorMateria)) return false;
        ProfesorMateria that = (ProfesorMateria) o;
        return getProfesor().equals(that.getProfesor()) &&
                getMateria().equals(that.getMateria()) &&
                getCurso().equals(that.getCurso());
    }

    public int hashCode() {
        return Objects.hash(getProfesor(), getMateria(), getCurso());
    }
}