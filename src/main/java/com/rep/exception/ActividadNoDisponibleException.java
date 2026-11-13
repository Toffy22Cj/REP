package com.rep.exception;

/** 409 — el estudiante no puede resolver la actividad (fuera de plazo, inactiva, sin reintentos...). */
public class ActividadNoDisponibleException extends OperacionNoPermitidaException {
    public ActividadNoDisponibleException(String message) {
        super(ErrorCode.ACTIVIDAD_NO_DISPONIBLE, message);
    }
}
