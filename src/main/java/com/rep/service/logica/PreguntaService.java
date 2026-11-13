package com.rep.service.logica;

import com.rep.dto.actividad.OpcionRequest;
import com.rep.dto.actividad.PreguntaRequest;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PreguntaService {
    Pregunta crearPregunta(PreguntaRequest request);

    Pregunta getPreguntaById(Long id);

    List<Pregunta> getPreguntasByActividad(Long actividadId);

    Pregunta actualizarPregunta(Long id, PreguntaRequest request);

    void eliminarPregunta(Long id);

    Opcion agregarOpcion(Long preguntaId, OpcionRequest request);

    Opcion getOpcionById(Long opcionId);

    List<Opcion> getOpcionesByPreguntaId(Long preguntaId);

    boolean profesorTieneAccesoAPregunta(Long profesorId, Long preguntaId);

    // ---- Adjuntos (imágenes, PDF, documentos, audio, vídeo...) ----

    Pregunta subirArchivo(Long preguntaId, MultipartFile file);

    void eliminarArchivo(Long preguntaId);

    Opcion subirArchivoOpcion(Long opcionId, MultipartFile file);

    ResponseEntity<Resource> descargarArchivo(Long preguntaId);

    ResponseEntity<Resource> descargarArchivoOpcion(Long opcionId);
}
