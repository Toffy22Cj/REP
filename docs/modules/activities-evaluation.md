# 📝 Módulo de Evaluación y Gestión de Actividades

## 1. Propósito y Alcance

El módulo de **Evaluación y Actividades** constituye el núcleo pedagógico e interactivo del sistema REP. Permite a los docentes diseñar instrumentos evaluativos estructurados (exámenes, quices y talleres), asociar reactivos con preguntas de diversa tipología y material multimedia/pictográfico, y permite a los estudiantes responder en línea con calificación y retroalimentación inmediata sobre preguntas objetivas.

---

## 2. Modelo de Datos y Entidades

```
┌─────────────────────────┐
│        Actividad        │
├─────────────────────────┤
│ id: Long (PK)           │
│ titulo: String          │
│ tipo: ENUM              │◄── 'EXAMEN', 'QUIZ', 'TALLER'
│ fechaEntrega: LocalDate │
│ horaEntrega: LocalTime  │──► Por defecto: 23:59:00
│ duracionMinutos: Integer│
│ permitirReintentos: bool│──► Por defecto: false
│ activa: Boolean         │──► Por defecto: true
└────────────┬────────────┘
             │1
             │*
┌────────────▼────────────┐
│        Pregunta         │
├─────────────────────────┤
│ id: Long (PK)           │
│ enunciado: String(500)  │
│ tipo: TipoPregunta      │◄── OPCION_MULTIPLE, VERDADERO_FALSO, RESPUESTA_ABIERTA
│ archivoUrl: String      │──► Ruta física en servidor
│ archivoTipo: String     │──► MIME type (ej. image/png)
└────────────┬────────────┘
             │1
             │*
┌────────────▼────────────┐
│         Opcion          │
├─────────────────────────┤
│ id: Long (PK)           │
│ texto: String(255)      │
│ esCorrecta: Boolean     │──► Exactamente 1 true por pregunta de opción múltiple
│ archivoUrl: String      │──► Pictograma / recurso gráfico
│ tieneArchivo: Boolean   │
└─────────────────────────┘
```

---

## 3. Reglas de Validación de Reactivos ([PreguntaServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/PreguntaServiceImpl.java))

1. **Límite de Opciones:** Cada pregunta de opción múltiple admite un máximo estricto de **5 opciones**. Al intentar agregar una sexta, se lanza `LimiteExcedidoException("Máximo 5 opciones por pregunta")`.
2. **Opción Correcta Única:** Al validar un conjunto de opciones múltiples, el sistema verifica que exista **exactamente una opción marcada como correcta**:
   ```java
   long opcionesCorrectas = opciones.stream().filter(OpcionRequest::getEsCorrecta).count();
   if (opcionesCorrectas != 1) {
       throw new ValidacionException("Debe haber exactamente una opción correcta");
   }
   ```
3. **Inmutabilidad tras Respuestas:** No se permite eliminar ni cambiar el tipo de una pregunta si ya existen entregas registradas en `respuestas_pregunta` (`respuestaPreguntaRepository.existsByPreguntaId(id)`), arrojando `OperacionNoPermitidaException`.

---

## 4. Motor de Corrección y Algoritmo de Calificación

La evaluación de una actividad se ejecuta de manera transaccional en [EstudianteServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/EstudianteServiceImpl.java) (`resolverActividad`):

### 4.1 Comprobaciones Previas de Autorización (`puedeRealizarActividad`)
Antes de aceptar un intento, el sistema valida rigurosamente:
1. Que el estudiante exista en la base de datos.
2. Que la actividad exista y esté marcada como `activa == true`.
3. Que el estudiante pertenezca formalmente al curso vinculado a la actividad:
   `cursoActividadId.equals(estudiante.getCurso().getId())`.
4. Que la fecha/hora actual no sea previa a la fecha de publicación de la actividad.
5. Que la fecha/hora actual no sobrepase el deadline (`actividad.getFechaHoraEntrega()`).
6. Que el estudiante no haya enviado una entrega previa si `permitirReintentos == false`.

### 4.2 Algoritmo de Cálculo de Nota Preliminar
El sistema evalúa de forma automática todas las preguntas objetivas (`OPCION_MULTIPLE` y `VERDADERO_FALSO`). Las preguntas de tipo `RESPUESTA_ABIERTA` se marcan como pendientes de revisión por el docente.

```java
private float calcularNotaPreliminar(RespuestaEstudiante respuesta, Actividad actividad) {
    long totalPreguntasObjetivas = actividad.getPreguntas().stream()
            .filter(p -> p.getTipo() != Pregunta.TipoPregunta.RESPUESTA_ABIERTA)
            .count();

    if (totalPreguntasObjetivas == 0) return 0f;

    long respuestasCorrectas = respuesta.getRespuestasPreguntas().stream()
            .filter(rp -> rp.getEsCorrecta() != null && rp.getEsCorrecta())
            .count();

    // Escala estándar de 0.0 a 5.0 puntos
    float nota = (float) (respuestasCorrectas * 5.0 / totalPreguntasObjetivas);
    return Math.min(Math.round(nota * 10) / 10.0f, 5.0f); // Redondeo a 1 decimal
}
```

### 4.3 Generación de Retroalimentación Contextual
Para cada pregunta respondida, el sistema construye una explicación inmediata que se retorna al estudiante:
- **Respuesta Correcta:** `"¡Correcto! " + opcion.getTexto()`.
- **Respuesta Incorrecta:** `"Incorrecto. La respuesta correcta era: " + textoOpcionEsperada`.
- **Respuesta Abierta:** `"Respuesta enviada: [...] (Requiere revisión del profesor)"`.

---

## 5. Gestión de Archivos Adjuntos y Recursos Multimedia

El sistema permite adjuntar imágenes, diagramas y pictogramas (orientados a accesibilidad pedagógica y comunicación aumentativa ARASAAC):

- **Directorio de Almacenamiento:** `$HOME/.rep/uploads/`.
- **Estrategia de Nombres Únicos:** Previene sobreescrituras accidentales ante archivos con nombres comunes:
  `"opcion_" + opcionId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename()`.
- **Límites de Carga de Archivos (Configurados en `application.properties`):**
  - Tamaño máximo por archivo: `10MB` (`spring.servlet.multipart.max-file-size=10MB`).
  - Tamaño máximo por petición: `20MB` (`spring.servlet.multipart.max-request-size=20MB`).
- **Descarga Segura y Autorizada:** Los endpoints `GET /api/preguntas/{id}/archivo` y `GET /api/preguntas/opciones/{opcionId}/archivo` verifican la identidad del solicitante:
  - Si es `PROFESOR`, debe ser el propietario legítimo de la actividad.
  - Si es `ESTUDIANTE`, debe pertenecer al curso asignado a la actividad evaluativa.
