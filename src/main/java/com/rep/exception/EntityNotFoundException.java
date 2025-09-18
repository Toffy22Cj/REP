package com.rep.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entity, Long id) {
        super(entity + " no encontrada con ID: " + id);
    }
}