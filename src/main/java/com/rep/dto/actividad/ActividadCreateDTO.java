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

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion = "";

    @NotNull(message = "La fecha de entrega es obligatoria")
    @FutureOrPresent(message = "La fecha de entrega no puede ser en el pasado")
    private LocalDate fechaEntrega;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración mínima es 1 minuto")
    @Max(value = 300, message = "La duración máxima es 300 minutos (5 horas)")
    private Integer duracionMinutos;

    // CAMBIOS AQUÍ: Eliminar profesorMateriaId y agregar materiaId y cursoId
    @NotNull(message = "La materia es obligatoria")
    private Long materiaId;

    @NotNull(message = "El curso es obligatorio")
    private Long cursoId;

    // El profesorId se obtendrá del token en el backend

    // Getters y Setters (Lombok @Data los genera automáticamente)
}