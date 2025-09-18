package com.rep.dto.profesor;

import lombok.Data;

@Data
public class ProfesorMateriaDTO {
    private Long profesorId;
    private Long materiaId;
    private Long cursoId;

    // Getters y Setters
    public Long getProfesorId() { return profesorId; }
    public void setProfesorId(Long profesorId) { this.profesorId = profesorId; }

    public Long getMateriaId() { return materiaId; }
    public void setMateriaId(Long materiaId) { this.materiaId = materiaId; }

    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
}