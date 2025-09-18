package com.rep.dto.actividad;

public class OpcionPlantillaDTO {
    private String texto;
    private Boolean esCorrecta;

    // Getters y Setters
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Boolean getEsCorrecta() {
        return esCorrecta;
    }

    public void setEsCorrecta(Boolean esCorrecta) {
        this.esCorrecta = esCorrecta;
    }
}