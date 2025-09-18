package com.rep.service.mapas;

import com.rep.model.Curso;
import com.rep.dto.curso.CursoDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CursoMapper {

    @Mapping(target = "cantidadEstudiantes", expression = "java(curso.getEstudiantes() != null ? (long) curso.getEstudiantes().size() : 0L)")
    CursoDTO toDto(Curso curso);

    @InheritInverseConfiguration
    @Mapping(target = "estudiantes", ignore = true)
    Curso toEntity(CursoDTO cursoDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estudiantes", ignore = true)
    void updateCursoFromDto(CursoDTO cursoDTO, @MappingTarget Curso curso);
}