package com.rep.dto.curso;

public class CursoDTO {

    private Long id;
    private String nombre;   // 👈 CLAVE
    private Integer grado;
    private String grupo;
    private Long cantidadEstudiantes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getGrado() { return grado; }
    public void setGrado(Integer grado) { this.grado = grado; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public Long getCantidadEstudiantes() { return cantidadEstudiantes; }
    public void setCantidadEstudiantes(Long cantidadEstudiantes) {
        this.cantidadEstudiantes = cantidadEstudiantes;
    }

    @Override
    public String toString() {
        return nombre != null ? nombre : "Curso";
    }
}
