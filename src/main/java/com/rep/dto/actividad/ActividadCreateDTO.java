package com.rep.dto.actividad;

import com.rep.model.Actividad.TipoActividad;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ActividadCreateDTO {
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 3, max = 100, message = "El título debe tener entre 3 y 100 caracteres")
    private String titulo;

    @NotNull(message = "El tipo de actividad es obligatorio")
    private TipoActividad tipo;

    private String descripcion;

    @NotNull(message = "La fecha de entrega es obligatoria")
    @FutureOrPresent(message = "La fecha de entrega no puede ser en el pasado")
    private LocalDate fechaEntrega;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración mínima es 1 minuto")
    @Max(value = 300, message = "La duración máxima es 300 minutos (5 horas)")
    private Integer duracionMinutos;

    @NotNull(message = "La relación profesor-materia es obligatoria")
    private Long profesorMateriaId;

    // Getters y Setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public TipoActividad getTipo() { return tipo; }
    public void setTipo(TipoActividad tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public Long getProfesorMateriaId() { return profesorMateriaId; }
    public void setProfesorMateriaId(Long profesorMateriaId) { this.profesorMateriaId = profesorMateriaId; }
}