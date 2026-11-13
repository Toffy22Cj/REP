# Catálogo de errores de la API

Todas las respuestas de error de la API (controladores, validación y filtros de
seguridad) tienen el mismo formato JSON (`ApiError`):

```json
{
  "timestamp": "2026-11-12T22:10:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "DOCUMENTO_LONGITUD_INVALIDA",
  "message": "La tarjeta de identidad (TI) debe tener exactamente 10 dígitos (tiene 6)",
  "path": "/api/admin/usuarios",
  "details": { "tipoIdentificacion": "TI", "identificacion": "363636", "longitud": 6 }
}
```

| Campo | Descripción |
|-------|-------------|
| `code` | Nombre estable del error. **Los clientes deben decidir con este campo**, no con el texto. |
| `message` | Texto en español listo para mostrar al usuario. |
| `details` | Datos adicionales (campo → mensaje en `CAMPOS_INVALIDOS`, valores en conflicto...). Opcional. |
| `traceId` | Solo en errores 5xx; búsquelo en el log del servidor para ver la traza completa. |

## Cómo añadir un error nuevo

1. Añada la constante a `com.rep.exception.ErrorCode` (estado HTTP + mensaje por defecto).
2. Lance la excepción con nombre adecuada: `ValidacionException` (400),
   `RecursoNoEncontradoException` (404), `ConflictoException` (409),
   `OperacionNoPermitidaException` (409), `AccessDeniedException` (403)… o una
   subclase específica (`DocumentoIdentidadInvalidoException`, `NotaFueraDeEscalaException`…).
3. No capture `Exception` en los controladores: `GlobalExceptionHandler` se encarga.
4. Añada la fila a este documento y un caso en `ApiErrorContractTest`.

## Catálogo


### Generales

| Código | HTTP | Mensaje por defecto |
|---|---|---|
| `VALIDACION_FALLIDA` | 400 | Los datos enviados no son válidos |
| `CAMPOS_INVALIDOS` | 400 | Uno o más campos no son válidos |
| `JSON_MALFORMADO` | 400 | El cuerpo de la petición no es un JSON válido o tiene valores no admitidos |
| `PARAMETRO_FALTANTE` | 400 | Falta un parámetro obligatorio |
| `PARAMETRO_INVALIDO` | 400 | Un parámetro tiene un formato no válido |
| `RECURSO_NO_ENCONTRADO` | 404 | El recurso solicitado no existe |
| `RUTA_NO_ENCONTRADA` | 404 | La ruta solicitada no existe |
| `METODO_NO_PERMITIDO` | 405 | Método HTTP no permitido para esta ruta |
| `CONFLICTO_DATOS` | 409 | La operación entra en conflicto con datos existentes |
| `OPERACION_NO_PERMITIDA` | 409 | La operación no está permitida en el estado actual |
| `LIMITE_EXCEDIDO` | 400 | Se ha superado un límite permitido |
| `ARCHIVO_DEMASIADO_GRANDE` | 413 | El archivo excede el tamaño máximo permitido |
| `DEMASIADAS_SOLICITUDES` | 429 | Demasiadas solicitudes. Intente más tarde |
| `ERROR_INTERNO` | 500 | Se produjo un error interno. Contacte a soporte con el traceId |

### Autenticación y autorización

| Código | HTTP | Mensaje por defecto |
|---|---|---|
| `NO_AUTENTICADO` | 401 | Debe iniciar sesión para acceder a este recurso |
| `TOKEN_INVALIDO` | 401 | La sesión no es válida o ha expirado |
| `CREDENCIALES_INVALIDAS` | 401 | Identificación o contraseña incorrectas |
| `CUENTA_INACTIVA` | 403 | La cuenta está inactiva. Contacte al administrador |
| `ACCESO_DENEGADO` | 403 | No tiene permisos para realizar esta acción |
| `REGISTRO_ROL_NO_PERMITIDO` | 403 | El registro público solo permite cuentas de estudiante |

### Usuarios

| Código | HTTP | Mensaje por defecto |
|---|---|---|
| `USUARIO_NO_ENCONTRADO` | 404 | El usuario no existe |
| `USUARIO_IDENTIFICACION_DUPLICADA` | 409 | Ya existe un usuario con esa identificación |
| `USUARIO_CORREO_DUPLICADO` | 409 | Ya existe un usuario con ese correo |
| `USUARIO_CAMBIO_ROL_NO_PERMITIDO` | 409 | No se puede cambiar el rol de profesores o estudiantes |
| `USUARIO_AUTOGESTION_NO_PERMITIDA` | 409 | Un administrador no puede desactivar ni eliminar su propia cuenta |
| `DOCUMENTO_CC_MENOR_DE_EDAD` | 400 | La cédula de ciudadanía (CC) solo es válida para mayores de 18 años |
| `DOCUMENTO_TI_MAYOR_DE_EDAD` | 400 | La tarjeta de identidad (TI) solo es válida para menores de 18 años |
| `DOCUMENTO_LONGITUD_INVALIDA` | 400 | La identificación no tiene la longitud válida para su tipo de documento |
| `DOCUMENTO_FORMATO_INVALIDO` | 400 | La identificación solo puede contener dígitos |
| `ESTUDIANTE_NO_ENCONTRADO` | 404 | El estudiante no existe |
| `ESTUDIANTE_SIN_CURSO` | 400 | El curso es obligatorio para estudiantes |
| `ESTUDIANTE_SIN_EDAD` | 400 | La edad es obligatoria para estudiantes |
| `ESTUDIANTE_YA_EN_CURSO` | 409 | El estudiante ya pertenece a ese curso |
| `PROFESOR_NO_ENCONTRADO` | 404 | El profesor no existe |
| `PROFESOR_CON_ASIGNACIONES` | 409 | No se puede eliminar un profesor con materias asignadas |
| `PROFESOR_ESTADO_INVALIDO` | 400 | Estado de profesor no válido. Use 'activo' o 'retirado' |

### Académico

| Código | HTTP | Mensaje por defecto |
|---|---|---|
| `CURSO_NO_ENCONTRADO` | 404 | El curso no existe |
| `CURSO_DUPLICADO` | 409 | Ya existe un curso con ese grado y grupo |
| `CURSO_CON_ESTUDIANTES` | 409 | No se puede eliminar un curso con estudiantes |
| `CURSO_CON_ASIGNACIONES` | 409 | No se puede eliminar un curso con materias asignadas |
| `MATERIA_NO_ENCONTRADA` | 404 | La materia no existe |
| `MATERIA_DUPLICADA` | 409 | Ya existe una materia con ese nombre |
| `MATERIA_CON_ASIGNACIONES` | 409 | No se puede eliminar una materia asignada a profesores |
| `ASIGNACION_NO_ENCONTRADA` | 404 | La asignación no existe |
| `ASIGNACION_DUPLICADA` | 409 | Esa asignación profesor-materia-curso ya existe |

### Actividades y evaluación

| Código | HTTP | Mensaje por defecto |
|---|---|---|
| `ACTIVIDAD_NO_ENCONTRADA` | 404 | La actividad no existe |
| `ACTIVIDAD_NO_DISPONIBLE` | 409 | La actividad no está disponible para este estudiante |
| `PREGUNTA_NO_ENCONTRADA` | 404 | La pregunta no existe |
| `OPCION_NO_PERTENECE_A_PREGUNTA` | 403 | La opción no pertenece a esta pregunta |
| `RESPUESTA_NO_ENCONTRADA` | 404 | La entrega no existe |
| `NOTA_FUERA_DE_ESCALA` | 400 | La nota debe estar entre 0.0 y 5.0 |
| `FORMATO_EXPORTACION_NO_SOPORTADO` | 400 | Formato de exportación no soportado |
| `REPORTE_NO_GENERADO` | 500 | No se pudo generar el reporte |
| `PREGUNTA_OPCIONES_INVALIDAS` | 400 | Las opciones de la pregunta no son válidas |

### Archivos y material de estudio

| Código | HTTP | Mensaje por defecto |
|---|---|---|
| `ARCHIVO_VACIO` | 400 | No se recibió ningún archivo o está vacío |
| `ARCHIVO_TIPO_NO_PERMITIDO` | 400 | Tipo de archivo no permitido |
| `ARCHIVO_NO_ENCONTRADO` | 404 | El archivo no existe |
| `ARCHIVO_NO_GUARDADO` | 500 | No se pudo guardar el archivo |
| `MATERIAL_NO_ENCONTRADO` | 404 | El material de estudio no existe |

## Reglas de documento de identidad

| Tipo | Edad | Longitud | Código si no se cumple |
|------|------|----------|------------------------|
| CC (cédula) | ≥ 18 | 6–10 dígitos | `DOCUMENTO_CC_MENOR_DE_EDAD` / `DOCUMENTO_LONGITUD_INVALIDA` |
| TI (tarjeta de identidad) | < 18 | exactamente 10 dígitos | `DOCUMENTO_TI_MAYOR_DE_EDAD` / `DOCUMENTO_LONGITUD_INVALIDA` |

Implementado en `DocumentoIdentidadPolicy` (backend) y `validacion.js` (panel web).
