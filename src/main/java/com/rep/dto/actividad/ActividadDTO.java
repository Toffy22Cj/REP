package com.rep.dto.actividad;

import com.rep.model.Actividad;
import com.rep.model.Actividad.TipoActividad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadDTO {
    private Long id;
    private Integer duracionMinutos;
    private String titulo;
    private TipoActividad tipo;
    private String descripcion;
    private LocalDate fechaEntrega;
    private LocalDateTime fechaCreacion;
    private Long profesorId;
    private Long materiaId;
    private String materiaNombre;
    private Long cursoId;
    private String cursoNombre;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public TipoActividad getTipo() { return tipo; }
    public void setTipo(TipoActividad tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Long getProfesorId() { return profesorId; }
    public void setProfesorId(Long profesorId) { this.profesorId = profesorId; }
    public Long getMateriaId() { return materiaId; }
    public void setMateriaId(Long materiaId) { this.materiaId = materiaId; }
    public String getMateriaNombre() { return materiaNombre; }
    public void setMateriaNombre(String materiaNombre) { this.materiaNombre = materiaNombre; }
    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }

    // Método para convertir de Entity a DTO
    public static ActividadDTO fromEntity(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();
        dto.setId(actividad.getId());
        dto.setTitulo(actividad.getTitulo());
        dto.setTipo(actividad.getTipo());
        dto.setDescripcion(actividad.getDescripcion());
        dto.setFechaEntrega(actividad.getFechaEntrega());
        dto.setDuracionMinutos(actividad.getDuracionMinutos());
        dto.setFechaCreacion(actividad.getFechaCreacion());

        if(actividad.getProfesorMateria() != null && actividad.getProfesorMateria().getProfesor() != null) {
            dto.setProfesorId(actividad.getProfesorMateria().getProfesor().getId());
        }

        if(actividad.getMateria() != null) {
            dto.setMateriaId(actividad.getMateria().getId());
            dto.setMateriaNombre(actividad.getMateria().getNombre());
        }

        if(actividad.getCurso() != null) {
            dto.setCursoId(actividad.getCurso().getId());
            dto.setCursoNombre(actividad.getCurso().getNombreCompleto());
        }

        return dto;
    }
}