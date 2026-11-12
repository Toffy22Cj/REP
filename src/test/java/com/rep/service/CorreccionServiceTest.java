package com.rep.service;

import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.RespuestaPregunta;
import com.rep.service.logica.CorreccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CorreccionService Unit Tests")
class CorreccionServiceTest {

    private CorreccionService correccionService;

    @BeforeEach
    void setUp() {
        correccionService = new CorreccionService();
    }

    @Test
    @DisplayName("esOpcionCorrecta debe retornar true solo si la opcion está marcada como correcta")
    void testEsOpcionCorrecta() {
        Opcion correcta = new Opcion();
        correcta.setEsCorrecta(true);

        Opcion incorrecta = new Opcion();
        incorrecta.setEsCorrecta(false);

        Opcion sinDefinir = new Opcion();
        sinDefinir.setEsCorrecta(null);

        assertTrue(correccionService.esOpcionCorrecta(correcta));
        assertFalse(correccionService.esOpcionCorrecta(incorrecta));
        assertFalse(correccionService.esOpcionCorrecta(sinDefinir));
        assertFalse(correccionService.esOpcionCorrecta(null));
    }

    @Test
    @DisplayName("calcularPuntuacionPregunta para opcion multiple retorna 1 si correcta, 0 si incorrecta")
    void testCalcularPuntuacionOpcionMultiple() {
        Pregunta pregunta = new Pregunta();
        pregunta.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);

        Opcion correcta = new Opcion();
        correcta.setEsCorrecta(true);

        RespuestaPregunta respuestaAcertada = new RespuestaPregunta();
        respuestaAcertada.setOpcion(correcta);

        float puntosAcertada = correccionService.calcularPuntuacionPregunta(pregunta, respuestaAcertada);
        assertEquals(1.0f, puntosAcertada);

        Opcion incorrecta = new Opcion();
        incorrecta.setEsCorrecta(false);

        RespuestaPregunta respuestaFallida = new RespuestaPregunta();
        respuestaFallida.setOpcion(incorrecta);

        float puntosFallida = correccionService.calcularPuntuacionPregunta(pregunta, respuestaFallida);
        assertEquals(0.0f, puntosFallida);
    }

    @Test
    @DisplayName("calcularPuntuacionPregunta para respuesta abierta retorna 0 porque requiere revision docente")
    void testCalcularPuntuacionRespuestaAbierta() {
        Pregunta pregunta = new Pregunta();
        pregunta.setTipo(Pregunta.TipoPregunta.RESPUESTA_ABIERTA);

        RespuestaPregunta respuesta = new RespuestaPregunta();
        respuesta.setRespuestaAbierta("Ensayo sobre la independencia");

        float puntos = correccionService.calcularPuntuacionPregunta(pregunta, respuesta);
        assertEquals(0.0f, puntos);
    }

    @Test
    @DisplayName("generarRetroalimentacion para opcion multiple muestra detalle de correccion o respuesta correcta")
    void testGenerarRetroalimentacionOpcionMultiple() {
        Pregunta pregunta = new Pregunta();
        pregunta.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);

        Opcion correcta = new Opcion();
        correcta.setTexto("Bogotá");
        correcta.setEsCorrecta(true);

        Opcion incorrecta = new Opcion();
        incorrecta.setTexto("Medellín");
        incorrecta.setEsCorrecta(false);

        pregunta.setOpciones(Set.of(correcta, incorrecta));

        // Caso respuesta correcta
        RespuestaPregunta respuestaCorrecta = new RespuestaPregunta();
        respuestaCorrecta.setOpcion(correcta);
        String retroCorrecta = correccionService.generarRetroalimentacion(pregunta, respuestaCorrecta);
        assertTrue(retroCorrecta.contains("¡Correcto!"));
        assertTrue(retroCorrecta.contains("Bogotá"));

        // Caso respuesta incorrecta
        RespuestaPregunta respuestaIncorrecta = new RespuestaPregunta();
        respuestaIncorrecta.setOpcion(incorrecta);
        String retroIncorrecta = correccionService.generarRetroalimentacion(pregunta, respuestaIncorrecta);
        assertTrue(retroIncorrecta.contains("Incorrecto"));
        assertTrue(retroIncorrecta.contains("Bogotá"));

        // Caso sin responder
        String retroSinResponder = correccionService.generarRetroalimentacion(pregunta, null);
        assertEquals("No respondió esta pregunta", retroSinResponder);
    }
}
