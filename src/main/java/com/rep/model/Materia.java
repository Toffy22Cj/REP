package com.rep.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "materias")
public class Materia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Agregar constructores
    public Materia() {}

    public Materia(Long id) {
        this.id = id;
    }

    public Materia(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Considerar agregar validación para nombre
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Column(nullable = false, unique = true)
    private String nombre;



    @Override
    public String toString() {
        return "Materia{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}