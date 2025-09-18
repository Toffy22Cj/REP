package com.rep.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recursos")
public class Recurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String tipo; // PDF, DOCX, etc.
    private String url; // Ruta del archivo en el servidor

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_materia_id", nullable = false)
    private ProfesorMateria profesorMateria;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;
}