package com.rep.service.logica;

/**
 * Escala de calificación institucional (0.0 a 5.0).
 *
 * Centraliza la regla que antes estaba implícita en el cálculo automático
 * ({@code * 5.0}) y que no se aplicaba a las calificaciones manuales, donde se
 * aceptaba cualquier valor (p. ej. -100 o 1000).
 */
public final class EscalaCalificacion {
    public static final float MINIMA = 0.0f;
    public static final float MAXIMA = 5.0f;

    private EscalaCalificacion() {
    }

    /** @throws com.rep.exception.NotaFueraDeEscalaException si la nota es nula o está fuera de la escala */
    public static float validar(Float nota) {
        if (nota == null || nota.isNaN() || nota < MINIMA || nota > MAXIMA) {
            throw new com.rep.exception.NotaFueraDeEscalaException(
                    "La nota debe estar entre " + MINIMA + " y " + MAXIMA + " (recibida: " + nota + ")");
        }
        return nota;
    }

    /**
     * Desempeño cualitativo (escala nacional del Decreto 1290: Superior, Alto, Básico,
     * Bajo). El umbral de Básico es la nota mínima aprobatoria de la institución.
     */
    public static String desempeno(Float nota, float minimaAprobatoria) {
        if (nota == null) {
            return null;
        }
        if (nota >= 4.6f) return "SUPERIOR";
        if (nota >= 4.0f) return "ALTO";
        if (nota >= minimaAprobatoria) return "BASICO";
        return "BAJO";
    }

    /** Redondeo institucional a una cifra decimal. */
    public static Float redondear(Double valor) {
        return valor == null ? null : Math.round(valor * 10) / 10f;
    }
}
