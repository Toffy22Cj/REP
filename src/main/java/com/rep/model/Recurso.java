package com.rep.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Material de estudio (archivo) publicado por un profesor para una materia en un curso.
 * Los estudiantes del curso pueden verlo y descargarlo.
 */
@Data
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "recursos")
public class Recurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título visible del material. */
    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    /** Tipo MIME (image/png, application/pdf...). */
    private String tipo;

    /** Ruta interna relativa al almacenamiento (nunca se expone). */
    @JsonIgnore
    private String url;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_materia_id", nullable = false)
    private ProfesorMateria profesorMateria;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;
}
