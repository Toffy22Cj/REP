package com.rep.dto.perfil;

import java.time.LocalDate;
import java.util.List;

/** Datos del propio usuario. Las asignaciones solo aplican a profesores y el curso a estudiantes. */
public record PerfilDTO(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String identificacion,
        String tipoIdentificacion,
        String rol,
        Integer edad,
        String sexo,
        LocalDate fechaIngreso,
        String curso,
        List<String> asignaciones) {
}
