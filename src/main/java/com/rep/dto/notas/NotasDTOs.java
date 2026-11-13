package com.rep.dto.notas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs del reporte de notas (boletín del estudiante y planilla del profesor). */
public final class NotasDTOs {
    private NotasDTOs() {
    }

    /** Estado de un estudiante frente a una actividad. */
    public enum EstadoNota {
        /** Abierta y sin empezar. */
        PENDIENTE,
        /** Empezada y sin entregar, plazo abierto. */
        EN_CURSO,
        /** Entregada; tiene preguntas abiertas aún no revisadas por el profesor (nota preliminar). */
        ENTREGADA,
        /** Entregada y calificada (automática o revisada). */
        CALIFICADA,
        /** Plazo vencido sin entrega. */
        VENCIDA
    }

    public record NotaActividad(Long actividadId, String titulo, String tipo, LocalDate fechaEntrega,
            EstadoNota estado, Float nota, String observaciones) {
    }

    public record ResumenAsistencia(long presentes, long ausencias, long excusas) {
        public static final ResumenAsistencia VACIO = new ResumenAsistencia(0, 0, 0);
    }

    public record NotasMateria(Long materiaId, String materia, String profesor, Float promedio, String desempeno,
            Boolean aprobada, int calificadas, int pendientes, ResumenAsistencia asistencia,
            List<NotaActividad> actividades) {
    }

    public record Boletin(Long estudianteId, String estudiante, String identificacion, String curso,
            LocalDateTime generadoEn, float notaMinimaAprobatoria, Float promedioGeneral, String desempenoGeneral,
            int materiasAprobadas, int materiasReprobadas, List<NotasMateria> materias) {
    }

    public record ColumnaActividad(Long actividadId, String titulo, String tipo, LocalDate fechaEntrega,
            Float promedio, int entregas) {
    }

    public record CeldaNota(Long actividadId, EstadoNota estado, Float nota) {
    }

    public record FilaEstudiante(Long estudianteId, String estudiante, String identificacion, List<CeldaNota> notas,
            Float promedio, String desempeno, Boolean aprobada, long ausencias) {
    }

    public record Planilla(Long cursoId, String curso, Long materiaId, String materia, LocalDateTime generadoEn,
            float notaMinimaAprobatoria, List<ColumnaActividad> actividades, List<FilaEstudiante> estudiantes,
            Float promedioCurso, int aprobados, int reprobados) {
    }
}
