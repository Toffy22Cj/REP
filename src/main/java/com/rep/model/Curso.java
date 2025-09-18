package com.rep.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "cursos")
@EqualsAndHashCode(exclude = {"estudiantes"}) // Excluir la lista de estudiantes
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(1) @Max(12)
    @Column(nullable = false)
    private Integer grado;

    @NotBlank
    @Size(max = 5)
    @Column(nullable = false)
    private String grupo;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore // Añade esto para evitar serialización
    private List<Estudiante> estudiantes = new ArrayList<>();

    @Transient
    public String getNombre() {
        return grado + "-" + grupo;
    }

    @Transient
    public String getNombreCompleto() {
        return grado + "-" + grupo;
    }

    // Método helper para relación bidireccional
    public void addEstudiante(Estudiante estudiante) {
        estudiantes.add(estudiante);
        if (estudiante.getCurso() != this) {
            estudiante.setCurso(this);
        }
    }
    // Agregar constructores
    public Curso() {}

    public Curso(Long id) {
        this.id = id;
    }




}