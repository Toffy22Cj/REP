package com.rep.dto.actividad;


import lombok.Data;

import java.util.List;

@Data
public class ActividadConPreguntasDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private List<PreguntaConOpcionesDTO> preguntas;

    /**
     * Elimina la información que un estudiante no debe ver antes de resolver la
     * actividad: qué opción es la correcta y las rutas internas de los archivos en el
     * servidor. Los adjuntos se exponen únicamente a través de los endpoints de
     * descarga, que aplican control de acceso.
     */
    public ActividadConPreguntasDTO ocultarSoluciones() {
        if (preguntas == null) {
            return this;
        }
        for (PreguntaConOpcionesDTO pregunta : preguntas) {
            pregunta.setArchivoUrl(pregunta.isArchivoDisponible()
                    ? "/api/preguntas/" + pregunta.getId() + "/archivo"
                    : null);
            if (pregunta.getOpciones() == null) {
                continue;
            }
            for (OpcionDTO opcion : pregunta.getOpciones()) {
                opcion.setEsCorrecta(null);
                opcion.setArchivoUrl(opcion.isArchivoDisponible()
                        ? "/api/preguntas/opciones/" + opcion.getId() + "/archivo"
                        : null);
            }
        }
        return this;
    }
}
