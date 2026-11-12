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

    /** @throws IllegalArgumentException si la nota es nula o está fuera de la escala */
    public static float validar(Float nota) {
        if (nota == null || nota.isNaN() || nota < MINIMA || nota > MAXIMA) {
            throw new IllegalArgumentException(
                    "La nota debe estar entre " + MINIMA + " y " + MAXIMA);
        }
        return nota;
    }
}
