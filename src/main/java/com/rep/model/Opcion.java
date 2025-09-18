package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(exclude = {"pregunta"})
@Entity
@Table(name = "opciones")
public class Opcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @Column(nullable = false, length = 255) // Añadido length para el texto
    private String texto;

    @Column(name = "es_correcta", nullable = false) // Añadido nullable=false
    private Boolean esCorrecta = false;

    // Método conveniente para marcado como correcta
    public void marcarComoCorrecta() {
        this.esCorrecta = true;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Opcion)) return false;
        Opcion opcion = (Opcion) o;
        return id != null && id.equals(opcion.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}