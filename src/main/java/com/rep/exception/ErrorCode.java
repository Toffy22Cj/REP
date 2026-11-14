package com.rep.exception;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de errores de la API.
 *
 * <p>Cada error tiene un nombre estable ({@link #name()}) que los clientes pueden usar
 * para reaccionar programáticamente (el texto del mensaje puede cambiar, el código no),
 * un estado HTTP y un mensaje por defecto en español. El catálogo completo está
 * documentado en {@code docs/errores.md}.</p>
 */
public enum ErrorCode {

    // ---------------------------------------------------------------- generales
    VALIDACION_FALLIDA(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos"),
    CAMPOS_INVALIDOS(HttpStatus.BAD_REQUEST, "Uno o más campos no son válidos"),
    JSON_MALFORMADO(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o tiene valores no admitidos"),
    PARAMETRO_FALTANTE(HttpStatus.BAD_REQUEST, "Falta un parámetro obligatorio"),
    PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST, "Un parámetro tiene un formato no válido"),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El recurso solicitado no existe"),
    RUTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "La ruta solicitada no existe"),
    METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta"),
    CONFLICTO_DATOS(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes"),
    OPERACION_NO_PERMITIDA(HttpStatus.CONFLICT, "La operación no está permitida en el estado actual"),
    LIMITE_EXCEDIDO(HttpStatus.BAD_REQUEST, "Se ha superado un límite permitido"),
    ARCHIVO_DEMASIADO_GRANDE(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo excede el tamaño máximo permitido"),
    DEMASIADAS_SOLICITUDES(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Intente más tarde"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Se produjo un error interno. Contacte a soporte con el traceId"),

    // ---------------------------------------------------------------- autenticación / autorización
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para acceder a este recurso"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "La sesión no es válida o ha expirado"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Identificación o contraseña incorrectas"),
    CUENTA_INACTIVA(HttpStatus.FORBIDDEN, "La cuenta está inactiva. Contacte al administrador"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción"),
    REGISTRO_ROL_NO_PERMITIDO(HttpStatus.FORBIDDEN, "El registro público solo permite cuentas de estudiante"),

    // ---------------------------------------------------------------- usuarios
    USUARIO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El usuario no existe"),
    CONTRASENA_ACTUAL_INCORRECTA(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta"),
    CONTRASENA_DEBIL(HttpStatus.BAD_REQUEST, "La nueva contraseña no cumple los requisitos de seguridad"),
    CONTRASENA_REPETIDA(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual"),
    USUARIO_IDENTIFICACION_DUPLICADA(HttpStatus.CONFLICT, "Ya existe un usuario con esa identificación"),
    USUARIO_CORREO_DUPLICADO(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo"),
    USUARIO_CAMBIO_ROL_NO_PERMITIDO(HttpStatus.CONFLICT, "No se puede cambiar el rol de profesores o estudiantes"),
    USUARIO_AUTOGESTION_NO_PERMITIDA(HttpStatus.CONFLICT, "Un administrador no puede desactivar ni eliminar su propia cuenta"),
    DOCUMENTO_CC_MENOR_DE_EDAD(HttpStatus.BAD_REQUEST, "La cédula de ciudadanía (CC) solo es válida para mayores de 18 años"),
    DOCUMENTO_TI_MAYOR_DE_EDAD(HttpStatus.BAD_REQUEST, "La tarjeta de identidad (TI) solo es válida para menores de 18 años"),
    DOCUMENTO_LONGITUD_INVALIDA(HttpStatus.BAD_REQUEST, "La identificación no tiene la longitud válida para su tipo de documento"),
    DOCUMENTO_FORMATO_INVALIDO(HttpStatus.BAD_REQUEST, "La identificación solo puede contener dígitos"),
    ESTUDIANTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El estudiante no existe"),
    ESTUDIANTE_SIN_CURSO(HttpStatus.BAD_REQUEST, "El curso es obligatorio para estudiantes"),
    ESTUDIANTE_SIN_EDAD(HttpStatus.BAD_REQUEST, "La edad es obligatoria para estudiantes"),
    ESTUDIANTE_YA_EN_CURSO(HttpStatus.CONFLICT, "El estudiante ya pertenece a ese curso"),
    PROFESOR_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El profesor no existe"),
    PROFESOR_CON_ASIGNACIONES(HttpStatus.CONFLICT, "No se puede eliminar un profesor con materias asignadas"),
    PROFESOR_ESTADO_INVALIDO(HttpStatus.BAD_REQUEST, "Estado de profesor no válido. Use 'activo' o 'retirado'"),

    // ---------------------------------------------------------------- académico
    CURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El curso no existe"),
    CURSO_DUPLICADO(HttpStatus.CONFLICT, "Ya existe un curso con ese grado y grupo"),
    CURSO_CON_ESTUDIANTES(HttpStatus.CONFLICT, "No se puede eliminar un curso con estudiantes"),
    CURSO_CON_ASIGNACIONES(HttpStatus.CONFLICT, "No se puede eliminar un curso con materias asignadas"),
    MATERIA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "La materia no existe"),
    MATERIA_DUPLICADA(HttpStatus.CONFLICT, "Ya existe una materia con ese nombre"),
    MATERIA_CON_ASIGNACIONES(HttpStatus.CONFLICT, "No se puede eliminar una materia asignada a profesores"),
    ASIGNACION_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "La asignación no existe"),
    ASIGNACION_DUPLICADA(HttpStatus.CONFLICT, "Esa asignación profesor-materia-curso ya existe"),
    PERIODO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El periodo académico no existe"),
    PERIODO_FECHAS_INVALIDAS(HttpStatus.BAD_REQUEST, "La fecha de inicio del periodo debe ser anterior a la de fin"),
    PERIODO_SOLAPADO(HttpStatus.CONFLICT, "Las fechas se cruzan con otro periodo del mismo año"),
    PERIODO_DUPLICADO(HttpStatus.CONFLICT, "Ya existe un periodo con ese número en el año"),
    PERIODO_PORCENTAJE_EXCEDIDO(HttpStatus.BAD_REQUEST, "La suma de porcentajes del año no puede superar 100 %"),

    // ---------------------------------------------------------------- actividades y evaluación
    ACTIVIDAD_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "La actividad no existe"),
    ACTIVIDAD_NO_DISPONIBLE(HttpStatus.CONFLICT, "La actividad no está disponible para este estudiante"),
    PREGUNTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "La pregunta no existe"),
    OPCION_NO_PERTENECE_A_PREGUNTA(HttpStatus.FORBIDDEN, "La opción no pertenece a esta pregunta"),
    RESPUESTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "La entrega no existe"),
    NOTA_FUERA_DE_ESCALA(HttpStatus.BAD_REQUEST, "La nota debe estar entre 0.0 y 5.0"),
    FORMATO_EXPORTACION_NO_SOPORTADO(HttpStatus.BAD_REQUEST, "Formato de exportación no soportado"),
    REPORTE_NO_GENERADO(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el reporte"),
    PREGUNTA_OPCIONES_INVALIDAS(HttpStatus.BAD_REQUEST, "Las opciones de la pregunta no son válidas"),

    // ---------------------------------------------------------------- archivos y material de estudio
    ARCHIVO_VACIO(HttpStatus.BAD_REQUEST, "No se recibió ningún archivo o está vacío"),
    ARCHIVO_TIPO_NO_PERMITIDO(HttpStatus.BAD_REQUEST, "Tipo de archivo no permitido"),
    ARCHIVO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El archivo no existe"),
    ARCHIVO_NO_GUARDADO(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo"),
    MATERIAL_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El material de estudio no existe");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
