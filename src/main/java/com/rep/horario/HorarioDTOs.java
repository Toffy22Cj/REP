package com.rep.horario;

import com.rep.model.BloqueHorario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public final class HorarioDTOs {
    private HorarioDTOs() {
    }

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    /** Bloque tal como lo ven los clientes. {@code dia}: 1 = lunes … 7 = domingo. */
    public record Bloque(Long id, Long asignacionId, int dia, String diaNombre, String horaInicio, String horaFin,
            String aula, Long materiaId, String materia, Long cursoId, String curso, Long profesorId, String profesor) {

        public static Bloque de(BloqueHorario b) {
            var pm = b.getProfesorMateria();
            String nombreDia = b.getDia().getDisplayName(TextStyle.FULL, ES);
            return new Bloque(b.getId(), pm.getId(), b.getDia().getValue(),
                    Character.toUpperCase(nombreDia.charAt(0)) + nombreDia.substring(1),
                    b.getHoraInicio().toString(), b.getHoraFin().toString(), b.getAula(),
                    pm.getMateria().getId(), pm.getMateria().getNombre(), pm.getCurso().getId(),
                    pm.getCurso().getNombre(), pm.getProfesor().getId(),
                    (pm.getProfesor().getNombre() + " " + pm.getProfesor().getApellido()).trim());
        }
    }

    /** Alta o edición de un bloque. {@code dia}: 1 = lunes … 7 = domingo. */
    public record GuardarBloque(
            @NotNull(message = "La asignación es obligatoria") Long asignacionId,
            @NotNull(message = "El día es obligatorio") Integer dia,
            @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
            @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
            @Size(max = 40, message = "Máximo 40 caracteres") String aula) {

        public DayOfWeek diaSemana() {
            return DayOfWeek.of(dia);
        }
    }
}
