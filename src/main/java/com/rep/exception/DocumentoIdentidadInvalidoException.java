package com.rep.exception;

import java.util.Map;

/** 400 — el tipo de documento no corresponde a la edad o la identificación tiene un formato inválido. */
public class DocumentoIdentidadInvalidoException extends ValidacionException {
    public DocumentoIdentidadInvalidoException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
