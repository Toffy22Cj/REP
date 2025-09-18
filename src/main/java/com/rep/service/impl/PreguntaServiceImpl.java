package com.rep.service.impl;

import com.rep.dto.actividad.*;
import com.rep.model.*;
import com.rep.repositories.*;
import com.rep.exception.*;
import com.rep.service.logica.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PreguntaServiceImpl implements PreguntaService {

    private final PreguntaRepository preguntaRepository;
    private final OpcionRepository opcionRepository;
    private final ActividadService actividadService;
    private final RespuestaPreguntaRepository respuestaPreguntaRepository;

    public PreguntaServiceImpl(PreguntaRepository preguntaRepository,
                               OpcionRepository opcionRepository,
                               ActividadService actividadService,
                               RespuestaPreguntaRepository respuestaPreguntaRepository) {
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.actividadService = actividadService;
        this.respuestaPreguntaRepository = respuestaPreguntaRepository;
    }

    @Override
    public Pregunta crearPregunta(PreguntaRequest request) {
        // Validar actividad
        Actividad actividad = actividadService.getActividadById(request.getActividadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        // Crear pregunta base
        Pregunta pregunta = new Pregunta();
        pregunta.setActividad(actividad);
        pregunta.setEnunciado(request.getEnunciado());
        pregunta.setTipo(request.getTipo());

        // Guardar pregunta
        Pregunta preguntaGuardada = preguntaRepository.save(pregunta);

        // Procesar opciones si es necesario
        if (request.getTipo() == Pregunta.TipoPregunta.OPCION_MULTIPLE && request.getOpciones() != null) {
            validarOpciones(request.getOpciones());
            guardarOpciones(preguntaGuardada, request.getOpciones());
        }

        return preguntaGuardada;
    }

    @Override
    public Pregunta getPreguntaById(Long id) {
        return preguntaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pregunta no encontrada con ID: " + id));
    }

    @Override
    public List<Pregunta> getPreguntasByActividad(Long actividadId) {
        return preguntaRepository.findByActividadIdOrderByIdAsc(actividadId);
    }

    @Override
    public Pregunta actualizarPregunta(Long id, PreguntaRequest request) {
        Pregunta preguntaExistente = getPreguntaById(id);

        // Validar cambio de tipo si ya tiene respuestas
        if (!preguntaExistente.getTipo().equals(request.getTipo())) {
            validarCambioTipoPregunta(preguntaExistente);
        }

        // Actualizar campos
        preguntaExistente.setEnunciado(request.getEnunciado());
        preguntaExistente.setTipo(request.getTipo());

        // Manejo especial para opciones múltiples
        if (request.getTipo() == Pregunta.TipoPregunta.OPCION_MULTIPLE) {
            actualizarOpciones(preguntaExistente, request.getOpciones());
        } else {
            // Eliminar opciones existentes si cambia de tipo
            opcionRepository.deleteByPreguntaId(preguntaExistente.getId());
        }

        return preguntaRepository.save(preguntaExistente);
    }

    @Override
    public void eliminarPregunta(Long id) {
        Pregunta pregunta = getPreguntaById(id);

        // Validar si tiene respuestas asociadas
        if (respuestaPreguntaRepository.existsByPreguntaId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la pregunta porque tiene respuestas asociadas"
            );
        }

        // Eliminar en cascada (configurado en la entidad)
        preguntaRepository.delete(pregunta);
    }

    @Override
    public Opcion agregarOpcion(Long preguntaId, OpcionRequest request) {
        Pregunta pregunta = getPreguntaById(preguntaId);

        // Validar tipo de pregunta
        if (pregunta.getTipo() != Pregunta.TipoPregunta.OPCION_MULTIPLE) {
            throw new OperacionNoPermitidaException(
                    "Solo preguntas de opción múltiple pueden tener opciones"
            );
        }

        // Validar límite de opciones
        if (opcionRepository.countByPreguntaId(preguntaId) >= 5) {
            throw new LimiteExcedidoException("Máximo 5 opciones por pregunta");
        }

        // Crear y guardar opción
        Opcion opcion = new Opcion();
        opcion.setPregunta(pregunta);
        opcion.setTexto(request.getTexto());
        opcion.setEsCorrecta(request.getEsCorrecta());

        return opcionRepository.save(opcion);
    }

    // --- Métodos auxiliares ---

    private void validarOpciones(List<OpcionRequest> opciones) {
        if (opciones == null || opciones.isEmpty()) {
            throw new ValidacionException("Las preguntas de opción múltiple deben tener al menos una opción");
        }

        long opcionesCorrectas = opciones.stream()
                .filter(OpcionRequest::getEsCorrecta)
                .count();

        if (opcionesCorrectas != 1) {
            throw new ValidacionException("Debe haber exactamente una opción correcta");
        }
    }

    private void guardarOpciones(Pregunta pregunta, List<OpcionRequest> opciones) {
        List<Opcion> opcionesEntities = opciones.stream()
                .map(opcionReq -> {
                    Opcion opcion = new Opcion();
                    opcion.setPregunta(pregunta);
                    opcion.setTexto(opcionReq.getTexto());
                    opcion.setEsCorrecta(opcionReq.getEsCorrecta());
                    return opcion;
                }).collect(Collectors.toList());

        opcionRepository.saveAll(opcionesEntities);
    }

    private void actualizarOpciones(Pregunta pregunta, List<OpcionRequest> nuevasOpciones) {
        // 1. Eliminar opciones existentes
        opcionRepository.deleteByPreguntaId(pregunta.getId());

        // 2. Guardar nuevas opciones
        if (nuevasOpciones != null && !nuevasOpciones.isEmpty()) {
            validarOpciones(nuevasOpciones);
            guardarOpciones(pregunta, nuevasOpciones);
        }
    }

    private void validarCambioTipoPregunta(Pregunta pregunta) {
        if (respuestaPreguntaRepository.existsByPreguntaId(pregunta.getId())) {
            throw new OperacionNoPermitidaException(
                    "No se puede cambiar el tipo de pregunta porque ya tiene respuestas asociadas"
            );
        }
    }

    // --- Métodos adicionales del contrato ---

    @Override
    public List<Opcion> getOpcionesByPreguntaId(Long preguntaId) {
        return opcionRepository.findByPreguntaId(preguntaId);
    }

    @Override
    public boolean profesorTieneAccesoAPregunta(Long profesorId, Long preguntaId) {
        return preguntaRepository.existsByIdAndActividad_ProfesorMateria_Profesor_Id(
                preguntaId,
                profesorId
        );
    }
}