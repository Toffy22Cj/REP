package com.rep.service.impl;

import com.rep.dto.actividad.OpcionRequest;
import com.rep.dto.actividad.PreguntaRequest;
import com.rep.exception.*;
import com.rep.model.Actividad;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.repositories.OpcionRepository;
import com.rep.repositories.PreguntaRepository;
import com.rep.repositories.RespuestaPreguntaRepository;
import com.rep.service.archivos.AlmacenamientoArchivosService;
import com.rep.service.archivos.AlmacenamientoArchivosService.ArchivoGuardado;
import com.rep.service.logica.ActividadService;
import com.rep.service.logica.PreguntaService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Preguntas de una actividad.
 *
 * <p>Reglas de opciones por tipo:</p>
 * <ul>
 *   <li>{@code OPCION_MULTIPLE}: entre 2 y {@value #MAX_OPCIONES} opciones con texto y
 *       exactamente una correcta.</li>
 *   <li>{@code VERDADERO_FALSO}: exactamente 2 opciones y una correcta. Antes las
 *       opciones de este tipo no se guardaban: el estudiante no podía responder y la
 *       pregunta contaba siempre como incorrecta.</li>
 *   <li>{@code RESPUESTA_ABIERTA}: sin opciones (se ignoran si se envían).</li>
 * </ul>
 */
@Service
@Transactional
public class PreguntaServiceImpl implements PreguntaService {

    static final int MAX_OPCIONES = 5;
    private static final String CARPETA_PREGUNTAS = "preguntas";
    private static final String CARPETA_OPCIONES = "opciones";

    private final PreguntaRepository preguntaRepository;
    private final OpcionRepository opcionRepository;
    private final ActividadService actividadService;
    private final RespuestaPreguntaRepository respuestaPreguntaRepository;
    private final AlmacenamientoArchivosService almacenamiento;

    public PreguntaServiceImpl(PreguntaRepository preguntaRepository,
            OpcionRepository opcionRepository,
            ActividadService actividadService,
            RespuestaPreguntaRepository respuestaPreguntaRepository,
            AlmacenamientoArchivosService almacenamiento) {
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.actividadService = actividadService;
        this.respuestaPreguntaRepository = respuestaPreguntaRepository;
        this.almacenamiento = almacenamiento;
    }

    @Override
    public Pregunta crearPregunta(PreguntaRequest request) {
        Actividad actividad = actividadService.getActividadById(request.getActividadId())
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ACTIVIDAD_NO_ENCONTRADA, request.getActividadId()));

        validarOpciones(request.getTipo(), request.getOpciones());

        Pregunta pregunta = new Pregunta();
        pregunta.setActividad(actividad);
        pregunta.setEnunciado(request.getEnunciado().trim());
        pregunta.setTipo(request.getTipo());
        agregarOpciones(pregunta, request.getTipo(), request.getOpciones());
        return preguntaRepository.save(pregunta);
    }

    @Override
    @Transactional(readOnly = true)
    public Pregunta getPreguntaById(Long id) {
        return preguntaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.PREGUNTA_NO_ENCONTRADA, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pregunta> getPreguntasByActividad(Long actividadId) {
        return preguntaRepository.findByActividadIdOrderByIdAsc(actividadId);
    }

    @Override
    public Pregunta actualizarPregunta(Long id, PreguntaRequest request) {
        Pregunta pregunta = getPreguntaById(id);
        if (pregunta.getTipo() != request.getTipo() && respuestaPreguntaRepository.existsByPreguntaId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede cambiar el tipo de pregunta porque ya tiene respuestas asociadas");
        }
        validarOpciones(request.getTipo(), request.getOpciones());

        pregunta.setEnunciado(request.getEnunciado().trim());
        pregunta.setTipo(request.getTipo());
        pregunta.getOpciones().clear(); // orphanRemoval elimina las anteriores
        agregarOpciones(pregunta, request.getTipo(), request.getOpciones());
        return preguntaRepository.save(pregunta);
    }

    @Override
    public void eliminarPregunta(Long id) {
        Pregunta pregunta = getPreguntaById(id);
        if (respuestaPreguntaRepository.existsByPreguntaId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la pregunta porque tiene respuestas asociadas");
        }
        almacenamiento.eliminar(pregunta.getArchivoUrl());
        pregunta.getOpciones().forEach(o -> almacenamiento.eliminar(o.getArchivoUrl()));
        preguntaRepository.delete(pregunta);
    }

    @Override
    public Opcion agregarOpcion(Long preguntaId, OpcionRequest request) {
        Pregunta pregunta = getPreguntaById(preguntaId);
        if (pregunta.getTipo() != Pregunta.TipoPregunta.OPCION_MULTIPLE) {
            throw new OperacionNoPermitidaException("Solo preguntas de opción múltiple admiten opciones adicionales");
        }
        if (opcionRepository.countByPreguntaId(preguntaId) >= MAX_OPCIONES) {
            throw new LimiteExcedidoException("Máximo " + MAX_OPCIONES + " opciones por pregunta");
        }
        Opcion opcion = new Opcion();
        opcion.setPregunta(pregunta);
        opcion.setTexto(request.getTexto());
        opcion.setEsCorrecta(Boolean.TRUE.equals(request.getEsCorrecta()));
        return opcionRepository.save(opcion);
    }

    @Override
    @Transactional(readOnly = true)
    public Opcion getOpcionById(Long opcionId) {
        return opcionRepository.findById(opcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La opción no existe (id " + opcionId + ")"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Opcion> getOpcionesByPreguntaId(Long preguntaId) {
        return opcionRepository.findByPreguntaId(preguntaId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean profesorTieneAccesoAPregunta(Long profesorId, Long preguntaId) {
        return preguntaRepository.existsByIdAndActividad_ProfesorMateria_Profesor_Id(preguntaId, profesorId);
    }

    // ------------------------------------------------------------------ adjuntos

    @Override
    public Pregunta subirArchivo(Long preguntaId, MultipartFile file) {
        Pregunta pregunta = getPreguntaById(preguntaId);
        ArchivoGuardado guardado = almacenamiento.guardar(file, CARPETA_PREGUNTAS);
        almacenamiento.eliminar(pregunta.getArchivoUrl()); // reemplaza el adjunto anterior
        pregunta.setArchivoUrl(guardado.ruta());
        pregunta.setArchivoTipo(guardado.tipoContenido());
        pregunta.setNombreArchivo(guardado.nombreOriginal());
        return preguntaRepository.save(pregunta);
    }

    @Override
    public void eliminarArchivo(Long preguntaId) {
        Pregunta pregunta = getPreguntaById(preguntaId);
        almacenamiento.eliminar(pregunta.getArchivoUrl());
        pregunta.setArchivoUrl(null);
        pregunta.setArchivoTipo(null);
        pregunta.setNombreArchivo(null);
        preguntaRepository.save(pregunta);
    }

    @Override
    public Opcion subirArchivoOpcion(Long opcionId, MultipartFile file) {
        Opcion opcion = getOpcionById(opcionId);
        ArchivoGuardado guardado = almacenamiento.guardar(file, CARPETA_OPCIONES);
        almacenamiento.eliminar(opcion.getArchivoUrl());
        opcion.setArchivoUrl(guardado.ruta());
        opcion.setArchivoTipo(guardado.tipoContenido());
        opcion.setNombreArchivo(guardado.nombreOriginal());
        opcion.setTieneArchivo(true);
        return opcionRepository.save(opcion);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargarArchivo(Long preguntaId) {
        Pregunta pregunta = getPreguntaById(preguntaId);
        return almacenamiento.descargar(pregunta.getArchivoUrl(), pregunta.getNombreArchivo(), pregunta.getArchivoTipo());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargarArchivoOpcion(Long opcionId) {
        Opcion opcion = getOpcionById(opcionId);
        return almacenamiento.descargar(opcion.getArchivoUrl(), opcion.getNombreArchivo(), opcion.getArchivoTipo());
    }

    // ------------------------------------------------------------------ opciones

    private static void validarOpciones(Pregunta.TipoPregunta tipo, List<OpcionRequest> opciones) {
        if (tipo == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
            return;
        }
        int cantidad = opciones == null ? 0 : opciones.size();
        if (tipo == Pregunta.TipoPregunta.VERDADERO_FALSO && cantidad != 2) {
            throw new ValidacionException(ErrorCode.PREGUNTA_OPCIONES_INVALIDAS,
                    "Una pregunta de verdadero/falso debe tener exactamente 2 opciones", Map.of("opciones", cantidad));
        }
        if (tipo == Pregunta.TipoPregunta.OPCION_MULTIPLE && (cantidad < 2 || cantidad > MAX_OPCIONES)) {
            throw new ValidacionException(ErrorCode.PREGUNTA_OPCIONES_INVALIDAS,
                    "Una pregunta de opción múltiple debe tener entre 2 y " + MAX_OPCIONES + " opciones",
                    Map.of("opciones", cantidad));
        }
        if (opciones.stream().anyMatch(o -> o.getTexto() == null || o.getTexto().isBlank())) {
            throw new ValidacionException(ErrorCode.PREGUNTA_OPCIONES_INVALIDAS, "Todas las opciones deben tener texto");
        }
        long correctas = opciones.stream().filter(o -> Boolean.TRUE.equals(o.getEsCorrecta())).count();
        if (correctas != 1) {
            throw new ValidacionException(ErrorCode.PREGUNTA_OPCIONES_INVALIDAS,
                    "Debe haber exactamente una opción correcta (hay " + correctas + ")", Map.of("correctas", correctas));
        }
    }

    private static void agregarOpciones(Pregunta pregunta, Pregunta.TipoPregunta tipo, List<OpcionRequest> opciones) {
        if (tipo == Pregunta.TipoPregunta.RESPUESTA_ABIERTA || opciones == null) {
            return;
        }
        for (OpcionRequest req : opciones) {
            Opcion opcion = new Opcion();
            opcion.setTexto(req.getTexto().trim());
            opcion.setEsCorrecta(Boolean.TRUE.equals(req.getEsCorrecta()));
            pregunta.addOpcion(opcion);
        }
    }
}
