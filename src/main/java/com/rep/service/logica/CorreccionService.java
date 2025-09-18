package com.rep.service.logica;

import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.RespuestaPregunta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CorreccionService {

    public boolean esOpcionCorrecta(Opcion opcion) {
        if (opcion == null) return false;
        return Boolean.TRUE.equals(opcion.getEsCorrecta());
    }

    public float calcularPuntuacionPregunta(Pregunta pregunta, RespuestaPregunta respuesta) {
        if (pregunta == null || respuesta == null) return 0f;

        switch(pregunta.getTipo()) {
            case OPCION_MULTIPLE:
            case VERDADERO_FALSO:
                return esOpcionCorrecta(respuesta.getOpcion()) ? 1f : 0f;
            case RESPUESTA_ABIERTA:
            default:
                return 0f; // Requiere corrección manual
        }
    }

    public String generarRetroalimentacion(Pregunta pregunta, RespuestaPregunta respuesta) {
        if (pregunta == null) return "Pregunta no disponible";

        if (respuesta == null) {
            return "No respondió esta pregunta";
        }

        switch(pregunta.getTipo()) {
            case OPCION_MULTIPLE:
            case VERDADERO_FALSO:
                if (respuesta.getOpcion() == null) {
                    return "No seleccionó ninguna opción";
                }
                boolean esCorrecta = esOpcionCorrecta(respuesta.getOpcion());
                return esCorrecta ?
                        "¡Correcto! " + respuesta.getOpcion().getTexto() :
                        "Incorrecto. La respuesta correcta era: " +
                                pregunta.getOpciones().stream()
                                        .filter(this::esOpcionCorrecta)
                                        .findFirst()
                                        .map(Opcion::getTexto)
                                        .orElse("No definida");

            case RESPUESTA_ABIERTA:
                String textoRespuesta = respuesta.getRespuestaAbierta() != null ?
                        respuesta.getRespuestaAbierta() : "[Vacía]";
                return "Respuesta enviada: " + textoRespuesta +
                        "\n(Requiere revisión del profesor)";

            default:
                return "Tipo de pregunta no soportado";
        }
    }
}