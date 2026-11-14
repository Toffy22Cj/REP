package com.rep.dto.notas;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Alta o edición de un periodo académico. */
public record PeriodoDTO(
        @NotNull(message = "El año es obligatorio") @Min(value = 2000, message = "Año no válido") @Max(value = 2100, message = "Año no válido") Integer anio,
        @NotNull(message = "El número es obligatorio") @Min(value = 1, message = "Debe ser 1 o mayor") @Max(value = 12, message = "Máximo 12 periodos") Integer numero,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 60, message = "Máximo 60 caracteres") String nombre,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        @NotNull(message = "El porcentaje es obligatorio") @Min(value = 1, message = "Mínimo 1 %") @Max(value = 100, message = "Máximo 100 %") Integer porcentaje) {
}
