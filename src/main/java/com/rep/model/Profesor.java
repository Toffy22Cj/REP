package com.rep.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "profesores")
@PrimaryKeyJoinColumn(name = "id")
public class Profesor extends Usuario {
// En Profesor.java
@Column(name = "edad", nullable = true)
private Integer edadProfesor;
    @Column(nullable = true)
    @Override
    public Integer getEdad() {
        return super.getEdad();
    }
    public Profesor(Long id) {
        this.setId(id); // Usar setId() heredado de Usuario
    }

    public Profesor() {}
    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    public enum EstadoProfesor {
        activo("activo"),  // Ahora en minúsculas para coincidir con la BD
        retirado("retirado");

        private final String descripcion;

        EstadoProfesor(String descripcion) {
            this.descripcion = descripcion;
        }

        @JsonCreator
        public static EstadoProfesor fromString(String value) {
            if (value == null) return null;
            for (EstadoProfesor estado : EstadoProfesor.values()) {
                if (estado.name().equalsIgnoreCase(value)) {
                    return estado;
                }
            }
            throw new IllegalArgumentException("Estado no válido: " + value);
        }

        @JsonValue
        public String toValue() {
            return this.name(); // Devuelve "activo" o "retirado"
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
    @Override
    public void setEdad(Integer edad) {
        super.setEdad(edad);
        this.edadProfesor = edad; // Mantener sincronizados ambos campos
    }
    // En la entidad Profesor:
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProfesor estado = EstadoProfesor.activo; // Valor por defecto en mayúsculas
}