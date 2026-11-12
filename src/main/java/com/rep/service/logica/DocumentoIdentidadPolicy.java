package com.rep.service.logica;

import com.rep.exception.DocumentoIdentidadInvalidoException;
import com.rep.exception.ErrorCode;

import java.util.Map;

/**
 * Reglas de documento de identidad (Colombia), antes duplicadas en el controlador:
 *
 * <ul>
 *   <li>Solo dígitos.</li>
 *   <li><b>CC</b> (cédula de ciudadanía): mayores de 18 años, entre 6 y 10 dígitos.</li>
 *   <li><b>TI</b> (tarjeta de identidad): menores de 18 años, exactamente 10 dígitos.</li>
 * </ul>
 */
public final class DocumentoIdentidadPolicy {
    public static final int MAYORIA_EDAD = 18;
    public static final int CC_MIN_DIGITOS = 6;
    public static final int CC_MAX_DIGITOS = 10;
    public static final int TI_DIGITOS = 10;

    private DocumentoIdentidadPolicy() {
    }

    public static void validar(String tipo, String identificacion, Integer edad) {
        if (identificacion == null || tipo == null) {
            return; // la obligatoriedad la validan las anotaciones del DTO
        }
        Map<String, Object> details = Map.of("tipoIdentificacion", tipo, "identificacion", identificacion,
                "longitud", identificacion.length());

        if (!identificacion.chars().allMatch(Character::isDigit)) {
            throw new DocumentoIdentidadInvalidoException(ErrorCode.DOCUMENTO_FORMATO_INVALIDO, null, details);
        }

        if ("CC".equals(tipo)) {
            if (edad != null && edad < MAYORIA_EDAD) {
                throw new DocumentoIdentidadInvalidoException(ErrorCode.DOCUMENTO_CC_MENOR_DE_EDAD,
                        "La cédula (CC) solo es válida para mayores de 18 años; para " + edad
                                + " años use tarjeta de identidad (TI)", details);
            }
            if (identificacion.length() < CC_MIN_DIGITOS || identificacion.length() > CC_MAX_DIGITOS) {
                throw new DocumentoIdentidadInvalidoException(ErrorCode.DOCUMENTO_LONGITUD_INVALIDA,
                        "La cédula (CC) debe tener entre " + CC_MIN_DIGITOS + " y " + CC_MAX_DIGITOS
                                + " dígitos (tiene " + identificacion.length() + ")", details);
            }
        } else if ("TI".equals(tipo)) {
            if (edad != null && edad >= MAYORIA_EDAD) {
                throw new DocumentoIdentidadInvalidoException(ErrorCode.DOCUMENTO_TI_MAYOR_DE_EDAD,
                        "La tarjeta de identidad (TI) solo es válida para menores de 18 años; para " + edad
                                + " años use cédula (CC)", details);
            }
            if (identificacion.length() != TI_DIGITOS) {
                throw new DocumentoIdentidadInvalidoException(ErrorCode.DOCUMENTO_LONGITUD_INVALIDA,
                        "La tarjeta de identidad (TI) debe tener exactamente " + TI_DIGITOS
                                + " dígitos (tiene " + identificacion.length() + ")", details);
            }
        }
    }
}
