package com.rep.service;

import com.rep.dto.actividad.OpcionRequest;
import com.rep.dto.actividad.PreguntaRequest;
import com.rep.exception.LimiteExcedidoException;
import com.rep.exception.OperacionNoPermitidaException;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.model.Actividad;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.repositories.OpcionRepository;
import com.rep.repositories.PreguntaRepository;
import com.rep.repositories.RespuestaPreguntaRepository;
import com.rep.service.impl.PreguntaServiceImpl;
import com.rep.service.logica.ActividadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreguntaService Unit Tests")
class PreguntaServiceTest {

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private OpcionRepository opcionRepository;

    @Mock
    private ActividadService actividadService;

    @Mock
    private RespuestaPreguntaRepository respuestaPreguntaRepository;

    private PreguntaServiceImpl preguntaService;

    @BeforeEach
    void setUp() {
        preguntaService = new PreguntaServiceImpl(
                preguntaRepository,
                opcionRepository,
                actividadService,
                respuestaPreguntaRepository
        );
    }

    @Test
    @DisplayName("eliminarPregunta lanza OperacionNoPermitidaException si la pregunta ya tiene respuestas")
    void testEliminarPreguntaConRespuestas() {
        Long preguntaId = 15L;
        Pregunta pregunta = new Pregunta();
        pregunta.setId(preguntaId);

        when(preguntaRepository.findById(preguntaId)).thenReturn(Optional.of(pregunta));
        when(respuestaPreguntaRepository.existsByPreguntaId(preguntaId)).thenReturn(true);

        assertThrows(OperacionNoPermitidaException.class, () -> preguntaService.eliminarPregunta(preguntaId));
        verify(preguntaRepository, never()).delete(any(Pregunta.class));
    }

    @Test
    @DisplayName("eliminarPregunta elimina correctamente si no tiene respuestas asociadas")
    void testEliminarPreguntaSinRespuestas() {
        Long preguntaId = 15L;
        Pregunta pregunta = new Pregunta();
        pregunta.setId(preguntaId);

        when(preguntaRepository.findById(preguntaId)).thenReturn(Optional.of(pregunta));
        when(respuestaPreguntaRepository.existsByPreguntaId(preguntaId)).thenReturn(false);

        preguntaService.eliminarPregunta(preguntaId);
        verify(preguntaRepository).delete(pregunta);
    }

    @Test
    @DisplayName("agregarOpcion lanza LimiteExcedidoException cuando ya hay 5 opciones")
    void testAgregarOpcionLimiteExcedido() {
        Long preguntaId = 10L;
        Pregunta pregunta = new Pregunta();
        pregunta.setId(preguntaId);
        pregunta.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);

        when(preguntaRepository.findById(preguntaId)).thenReturn(Optional.of(pregunta));
        when(opcionRepository.countByPreguntaId(preguntaId)).thenReturn(5L);

        OpcionRequest request = new OpcionRequest();
        request.setTexto("Opción 6");
        request.setEsCorrecta(false);

        assertThrows(LimiteExcedidoException.class, () -> preguntaService.agregarOpcion(preguntaId, request));
    }

    @Test
    @DisplayName("agregarOpcion lanza OperacionNoPermitidaException en preguntas de respuesta abierta")
    void testAgregarOpcionTipoIncompatible() {
        Long preguntaId = 10L;
        Pregunta pregunta = new Pregunta();
        pregunta.setId(preguntaId);
        pregunta.setTipo(Pregunta.TipoPregunta.RESPUESTA_ABIERTA);

        when(preguntaRepository.findById(preguntaId)).thenReturn(Optional.of(pregunta));

        OpcionRequest request = new OpcionRequest();
        request.setTexto("Opción A");

        assertThrows(OperacionNoPermitidaException.class, () -> preguntaService.agregarOpcion(preguntaId, request));
    }

    @Test
    @DisplayName("crearPregunta con opcion multiple valida que exista exactamente una opcion correcta")
    void testCrearPreguntaSinOpcionCorrecta() {
        Long actividadId = 1L;
        Actividad actividad = new Actividad();
        actividad.setId(actividadId);

        when(actividadService.getActividadById(actividadId)).thenReturn(Optional.of(actividad));

        PreguntaRequest request = new PreguntaRequest();
        request.setActividadId(actividadId);
        request.setEnunciado("¿Capital de Colombia?");
        request.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);

        List<OpcionRequest> opciones = new ArrayList<>();
        OpcionRequest op1 = new OpcionRequest();
        op1.setTexto("Medellín");
        op1.setEsCorrecta(false);

        OpcionRequest op2 = new OpcionRequest();
        op2.setTexto("Cali");
        op2.setEsCorrecta(false);

        opciones.add(op1);
        opciones.add(op2);
        request.setOpciones(opciones);

        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(ValidacionException.class, () -> preguntaService.crearPregunta(request));
    }

    @Test
    @DisplayName("getPreguntaById lanza RecursoNoEncontradoException cuando no existe")
    void testGetPreguntaByIdNotFound() {
        when(preguntaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> preguntaService.getPreguntaById(99L));
    }
}
