package com.rep.dto.actividad;


import lombok.Data;

import java.util.List;

@Data
public class ActividadConPreguntasDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    /** Tiempo para resolverla (el cliente muestra la cuenta regresiva). */
    private Integer duracionMinutos;
    private java.time.LocalDate fechaEntrega;
    private List<PreguntaConOpcionesDTO> preguntas;

    /**
     * Elimina la información que un estudiante no debe ver antes de resolver la
     * actividad: qué opción es la correcta y las rutas internas de los archivos en el
     * servidor. Los adjuntos se exponen únicamente a través de los endpoints de
     * descarga, que aplican control de acceso.
     */
    /**
     * Elimina la información que un estudiante no debe ver antes de resolver la
     * actividad: qué opción es la correcta. Los adjuntos ya se exponen solo como URL de
     * descarga de la API, que aplica control de acceso.
     */
    public ActividadConPreguntasDTO ocultarSoluciones() {
        if (preguntas != null) {
            preguntas.stream()
                    .filter(p -> p.getOpciones() != null)
                    .flatMap(p -> p.getOpciones().stream())
                    .forEach(o -> o.setEsCorrecta(null));
        }
        return this;
    }
}
