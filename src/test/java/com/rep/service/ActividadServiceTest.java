package com.rep.service;

import com.rep.exception.RecursoNoEncontradoException;
import com.rep.model.Actividad;
import com.rep.model.Curso;
import com.rep.model.Materia;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.Profesor;
import com.rep.model.ProfesorMateria;
import com.rep.model.RespuestaEstudiante;
import com.rep.repositories.ActividadRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.RespuestaEstudianteRepository;
import com.rep.service.impl.ActividadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActividadService Unit Tests")
class ActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private RespuestaEstudianteRepository respuestaEstudianteRepository;

    @Mock
    private ProfesorMateriaRepository profesorMateriaRepository;

    @Mock
    private com.rep.repositories.EstudianteRepository estudianteRepository;

    private ActividadServiceImpl actividadService;

    @BeforeEach
    void setUp() {
        actividadService = new ActividadServiceImpl(
                actividadRepository,
                respuestaEstudianteRepository,
                profesorMateriaRepository,
                estudianteRepository
        );
    }

    @Test
    @DisplayName("clonarActividad duplica profundamente la actividad, sus preguntas y opciones")
    void testClonarActividadDeepCopy() {
        Long actividadId = 1L;

        Profesor profesor = new Profesor();
        profesor.setId(5L);

        Materia materia = new Materia();
        materia.setId(10L);

        Curso curso = new Curso();
        curso.setId(20L);

        ProfesorMateria pm = new ProfesorMateria();
        pm.setId(100L);
        pm.setProfesor(profesor);
        pm.setMateria(materia);
        pm.setCurso(curso);

        Actividad original = new Actividad();
        original.setId(actividadId);
        original.setTitulo("Evaluación de Historia");
        original.setTipo(Actividad.TipoActividad.EXAMEN);
        original.setDescripcion("Cuestionario primer corte");
        original.setFechaEntrega(LocalDate.of(2026, 12, 1));
        original.setHoraEntrega(LocalTime.of(18, 0));
        original.setDuracionMinutos(60);
        original.setProfesorMateria(pm);
        original.setPermitirReintentos(false);

        // Pregunta con opción
        Pregunta pregunta1 = new Pregunta();
        pregunta1.setId(50L);
        pregunta1.setEnunciado("¿Año de la batalla de Boyacá?");
        pregunta1.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);
        pregunta1.setActividad(original);

        Opcion op1 = new Opcion();
        op1.setId(501L);
        op1.setTexto("1819");
        op1.setEsCorrecta(true);
        op1.setPregunta(pregunta1);

        pregunta1.setOpciones(new HashSet<>(Set.of(op1)));
        original.setPreguntas(new HashSet<>(Set.of(pregunta1)));

        when(actividadRepository.findByIdWithPreguntasAndOpciones(actividadId)).thenReturn(Optional.of(original));
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(invocation -> {
            Actividad saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        Actividad clon = actividadService.clonarActividad(actividadId);

        assertNotNull(clon);
        assertEquals("Evaluación de Historia (Copia)", clon.getTitulo());
        assertEquals(original.getTipo(), clon.getTipo());
        assertEquals(original.getDescripcion(), clon.getDescripcion());
        assertEquals(pm, clon.getProfesorMateria());
        assertTrue(clon.getActiva());
        assertEquals(1, clon.getPreguntas().size());

        Pregunta preguntaClon = clon.getPreguntas().iterator().next();
        assertNull(preguntaClon.getId(), "El ID de la pregunta clonada debe ser nulo para generar nuevo registro");
        assertEquals("¿Año de la batalla de Boyacá?", preguntaClon.getEnunciado());
        assertSame(clon, preguntaClon.getActividad(), "La pregunta debe apuntar al clon");

        assertEquals(1, preguntaClon.getOpciones().size());
        Opcion opcionClon = preguntaClon.getOpciones().iterator().next();
        assertNull(opcionClon.getId(), "El ID de la opción clonada debe ser nulo");
        assertEquals("1819", opcionClon.getTexto());
        assertTrue(opcionClon.getEsCorrecta());
        assertSame(preguntaClon, opcionClon.getPregunta(), "La opción debe apuntar a la pregunta clon");

        verify(actividadRepository).save(any(Actividad.class));
    }

    @Test
    @DisplayName("clonarActividad lanza RecursoNoEncontradoException cuando la actividad no existe")
    void testClonarActividadNoEncontrada() {
        when(actividadRepository.findByIdWithPreguntasAndOpciones(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> actividadService.clonarActividad(999L));
    }

    @Test
    @DisplayName("crearActividad valida título y asociación con ProfesorMateria")
    void testCrearActividadValidaciones() {
        // Título vacío
        Actividad sinTitulo = new Actividad();
        sinTitulo.setTitulo("   ");
        assertThrows(IllegalArgumentException.class, () -> actividadService.crearActividad(sinTitulo));

        // Sin ProfesorMateria
        Actividad sinPm = new Actividad();
        sinPm.setTitulo("Examen Parcial");
        sinPm.setProfesorMateria(null);
        assertThrows(IllegalArgumentException.class, () -> actividadService.crearActividad(sinPm));
    }

    @Test
    @DisplayName("actualizarNota modifica la calificación y observaciones de una respuesta")
    void testActualizarNota() {
        RespuestaEstudiante respuesta = new RespuestaEstudiante();
        respuesta.setId(30L);
        respuesta.setNota(3.0f);
        respuesta.setObservaciones("Trabajo inicial");

        when(respuestaEstudianteRepository.findByEstudianteIdAndActividadId(10L, 20L))
                .thenReturn(Optional.of(respuesta));
        when(respuestaEstudianteRepository.save(any(RespuestaEstudiante.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RespuestaEstudiante actualizada = actividadService.actualizarNota(20L, 10L, 4.8f, "Excelente corrección");

        assertEquals(4.8f, actualizada.getNota());
        assertEquals("Excelente corrección", actualizada.getObservaciones());
    }

    @Test
    @DisplayName("actualizarNota rechaza notas fuera de la escala 0-5")
    void testActualizarNotaFueraDeEscala() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> actividadService.actualizarNota(20L, 10L, 7.5f, null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> actividadService.actualizarNota(20L, 10L, -1f, null));
        org.mockito.Mockito.verifyNoInteractions(respuestaEstudianteRepository);
    }
}
