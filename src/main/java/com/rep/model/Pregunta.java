package com.rep.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = {"actividad", "opciones"})
@Table(name = "preguntas")
@Entity
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum TipoPregunta {
        OPCION_MULTIPLE,
        VERDADERO_FALSO,
        RESPUESTA_ABIERTA
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(nullable = false, length = 500) // Añadido length para el enunciado
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) // Añadido nullable=false y length
    private TipoPregunta tipo;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Opcion> opciones = new HashSet<>(); // Cambia List por Set
    // Método helper para añadir opciones
    public void addOpcion(Opcion opcion) {
        opciones.add(opcion);
        opcion.setPregunta(this);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pregunta)) return false;
        Pregunta pregunta = (Pregunta) o;
        return id != null && id.equals(pregunta.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}