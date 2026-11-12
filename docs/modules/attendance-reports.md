# 📋 Módulo de Asistencia y Generación de Reportes

## 1. Propósito y Alcance

Este módulo cubre dos requerimientos operativos esenciales para la gestión institucional:
1. **Control Diario de Asistencia Escolar:** Registro por sesión de clase con estados normalizados, tipos de excusa y observaciones para cada estudiante matriculado.
2. **Motor Polifacético de Reportabilidad:** Generación de documentos binarios en formatos Microsoft Excel (`.xlsx`), Adobe PDF (`.pdf`) y texto delimitado (`.csv`) para boletines de notas y auditoría directiva.

---

## 2. Modelo de Control de Asistencias

```
┌────────────────────────────────────────────────────────┐
│                      Asistencia                        │
├────────────────────────────────────────────────────────┤
│ id: Long (PK)                                          │
│ fecha: LocalDate                                       │
│ estado: EstadoAsistencia (PRESENTE, NO_INGRESO, EXCUSA)│
│ tipo_excusa: String                                    │
│ observacion: String (TEXT)                             │
│ estudiante_id: FK -> estudiantes.id                    │
│ profesor_materia_id: FK -> profesor_materia.id         │
└────────────────────────────────────────────────────────┘
  Restricción Única: (profesor_materia_id, estudiante_id, fecha)
```

### 2.1 Garantía de Idempotencia y Actualización ([AsistenciaServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/AsistenciaServiceImpl.java))
Al registrar la asistencia de una clase (`saveAsistencias`), el servicio no asume que sea una inserción limpia. Para cada estudiante recibido en el listado `List<AsistenciaDTO>`:
1. Consulta si ya existe un registro para la tríada `(profesor_materia_id, estudiante_id, fecha)`.
2. Si existe, actualiza el estado, excusa y observaciones existentes.
3. Si no existe, inicializa una nueva entidad vinculando la clave foránea.
Esto permite al docente corregir o reabrir el pase de lista de un día sin causar duplicidades ni errores de clave foránea.

---

## 3. Motores de Reportes Binarios

El sistema integra internamente tres tecnologías de generación de reportes:

```
                  ┌──────────────────────────────────────────────┐
                  │          Petición de Exportación             │
                  └──────────────────────┬───────────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    ▼                    ▼                    ▼
             Formato: "xlsx"      Formato: "pdf"        Formato: "csv"
                    │                    │                    │
                    ▼                    ▼                    ▼
             ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
             │  Apache POI  │     │   iText 7    │     │ Java String  │
             │   (OOXML)    │     │  Kernel/Doc  │     │   RFC 4180   │
             └──────────────┘     └──────────────┘     └──────────────┘
```

### 3.1 Motor Excel con Apache POI ([ExcelReportGenerator.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/archivos/ExcelReportGenerator.java))
- **Clases:** `XSSFWorkbook`, `Sheet`, `Row`, `Cell`, `CellStyle`, `Font`.
- **Estructura Visual:**
  - Pestaña de trabajo nombrada `"Resultados"`.
  - Encabezados estilizados con fondo color azul claro (`IndexedColors.LIGHT_BLUE`), patrón de relleno sólido y tipografía en negrita (`Font.setBold(true)`).
  - Ajuste automático de ancho de columna mediante `sheet.autoSizeColumn(0)` y `sheet.autoSizeColumn(1)`.
- **Salida:** `byte[]` transmitido con `Content-Type: application/octet-stream` o `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

### 3.2 Motor PDF Moderno con iText 7 ([PdfReportGenerator.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/archivos/PdfReportGenerator.java))
- **Paquetes:** `com.itextpdf.kernel.pdf.*`, `com.itextpdf.layout.*`.
- **Diseño Documental:**
  - Título centralizado con fuente de 16pt en negrita: `"Resultados de la Actividad: " + titulo`.
  - Objeto `Table(2)` con encabezados de celda (`"Estudiante"` y `"Calificación"`).
  - Iteración de registros formateando la nota decimal a dos dígitos (`String.format("%.2f", respuesta.getNota())`).

### 3.3 Motor PDF de Promedios con OpenPDF ([RespuestaServiceImpl.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/service/impl/RespuestaServiceImpl.java))
- **Paquetes:** `com.lowagie.text.*`, `com.lowagie.text.pdf.*`.
- **Propósito:** Genera la sábana de notas consolidadas de un curso completo en una asignatura particular (`generatePromediosPdfReport(cursoId, materiaId)`).
- **Estructura:** Tabla de 2 columnas (`"Actividad ID"` y `"Promedio"`), iterando agregaciones matemáticas SQL calculadas por Hibernate.
