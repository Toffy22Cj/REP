package com.rep.service.logica;

import com.rep.dto.actividad.OpcionRequest;
import com.rep.dto.actividad.PreguntaPlantillaDTO;
import com.rep.dto.actividad.PreguntaRequest;
import com.rep.model.*;
import java.util.List;
import java.util.Map;

public interface PreguntaService {
        Pregunta crearPregunta(PreguntaRequest request);
        Pregunta getPreguntaById(Long id);
        List<Pregunta> getPreguntasByActividad(Long actividadId);
        Pregunta actualizarPregunta(Long id, PreguntaRequest request);
        void eliminarPregunta(Long id);
        Opcion agregarOpcion(Long preguntaId, OpcionRequest request);
        List<Opcion> getOpcionesByPreguntaId(Long preguntaId);
        boolean profesorTieneAccesoAPregunta(Long profesorId, Long preguntaId);
//        boolean profesorTieneAccesoAOpcion(Long profesorId, Long opcionId);
    }
