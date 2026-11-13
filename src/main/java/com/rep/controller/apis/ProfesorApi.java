package com.rep.controller.apis;

import com.rep.dto.curso.CursoDTO;
import com.rep.model.*;
import com.rep.repositories.CursoRepository;
import com.rep.repositories.MateriaRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.ProfesorRepository;
import com.rep.service.logica.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profesor")
public class ProfesorApi {

    private static final java.util.Set<String> FORMATOS_EXPORTACION = java.util.Set.of("pdf", "xlsx");

    private final MateriaService materiaService;
    private final CursoService cursoService;
    private final ActividadService actividadService;
    private final EstudianteService estudianteService;
    private final RespuestaService respuestaService;
    private final ValidacionService validacionService;
    private final AsistenciaService asistenciaService;
    private final NotasService notasService;
    private final com.rep.repositories.ProfesorMateriaRepository profesorMateriaRepository;
    private final com.rep.service.archivos.NotasReportGenerator reportes;

    public ProfesorApi(MateriaService materiaService,
                       CursoService cursoService,
                       ActividadService actividadService,
                       EstudianteService estudianteService,
                       RespuestaService respuestaService,
                       ValidacionService validacionService,
                       AsistenciaService asistenciaService,
                       NotasService notasService,
                       com.rep.service.archivos.NotasReportGenerator reportes,
                       com.rep.repositories.ProfesorMateriaRepository profesorMateriaRepository) {
        this.materiaService = materiaService;
        this.cursoService = cursoService;
        this.actividadService = actividadService;
        this.estudianteService = estudianteService;
        this.respuestaService = respuestaService;
        this.validacionService = validacionService;
        this.asistenciaService = asistenciaService;
        this.notasService = notasService;
        this.reportes = reportes;
        this.profesorMateriaRepository = profesorMateriaRepository;
    }

    /** Asignaciones del profesor (materia + curso) para los selectores del cliente. */
    @GetMapping("/asignaciones")
    public List<Map<String, Object>> asignaciones(@AuthenticationPrincipal Usuario usuario) {
        return profesorMateriaRepository.findByProfesorId(usuario.getId()).stream()
                .sorted(java.util.Comparator.comparing((ProfesorMateria pm) -> pm.getMateria().getNombre())
                        .thenComparing(pm -> pm.getCurso().getNombre()))
                .map(pm -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", pm.getId());
                    m.put("materiaId", pm.getMateria().getId());
                    m.put("materia", pm.getMateria().getNombre());
                    m.put("cursoId", pm.getCurso().getId());
                    m.put("curso", pm.getCurso().getNombre());
                    m.put("nombre", pm.getMateria().getNombre() + " · " + pm.getCurso().getNombre());
                    return m;
                })
                .toList();
    }

    // Planilla de notas de un curso y materia asignados
    @GetMapping("/notas")
    public com.rep.dto.notas.NotasDTOs.Planilla planillaNotas(@RequestParam Long cursoId, @RequestParam Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        return notasService.planilla(cursoId, materiaId);
    }

    @GetMapping("/notas/export")
    public ResponseEntity<byte[]> exportarPlanilla(@RequestParam Long cursoId, @RequestParam Long materiaId,
            @RequestParam(defaultValue = "xlsx") String formato, @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        var planilla = notasService.planilla(cursoId, materiaId);
        String base = ("notas-" + planilla.materia() + "-" + planilla.curso()).replaceAll("[^A-Za-z0-9._-]", "_");
        return switch (formato.toLowerCase(java.util.Locale.ROOT)) {
            case "pdf" -> archivo(reportes.planillaPdf(planilla), base + ".pdf", "application/pdf");
            case "xlsx" -> archivo(reportes.planillaExcel(planilla), base + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> throw new com.rep.exception.ValidacionException(
                    com.rep.exception.ErrorCode.FORMATO_EXPORTACION_NO_SOPORTADO, "Use formato=xlsx o formato=pdf");
        };
    }

    /**
     * Entregas de una actividad en formato resumido (ruta que usa el cliente de
     * escritorio; antes no existía y la pantalla de calificar quedaba vacía).
     */
    @GetMapping("/actividades/{actividadId}/respuestas")
    public List<Map<String, Object>> entregasDeActividad(@PathVariable Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), actividadId);
        return respuestaService.getRespuestasByActividad(actividadId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getEntregado()))
                .sorted(java.util.Comparator.comparing(r -> r.getEstudiante().getApellido(), String.CASE_INSENSITIVE_ORDER))
                .map(r -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("estudianteId", r.getEstudiante().getId());
                    m.put("nombreEstudiante", (r.getEstudiante().getNombre() + " " + r.getEstudiante().getApellido()).trim());
                    m.put("nota", r.getNota());
                    m.put("fechaEntrega", r.getFechaEntrega());
                    m.put("calificada", r.getCalificadoPor() != null);
                    m.put("observaciones", r.getObservaciones());
                    return m;
                })
                .toList();
    }

    private static ResponseEntity<byte[]> archivo(byte[] datos, String nombre, String tipo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename(nombre).build().toString())
                .body(datos);
    }

    // 1. Gestión de Materias
    @GetMapping("/materias")
    public ResponseEntity<?> getMateriasAsignadas(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(materiaService.getMateriasByProfesor(usuario.getId()));
    }

    // 2. Gestión de Cursos/Grupos
    // En ProfesorApi.java
    @GetMapping("/cursos")
    public ResponseEntity<List<CursoDTO>> getCursosByProfesor(@AuthenticationPrincipal Usuario usuario) {
        List<CursoDTO> cursos = cursoService.getCursosByProfesor(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    // 3. Revisión de Resultados
    @GetMapping("/respuestas/actividad/{actividadId}")
    public ResponseEntity<List<RespuestaEstudiante>> getRespuestasByActividad(@PathVariable Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), actividadId);
        List<RespuestaEstudiante> respuestas = respuestaService.getRespuestasByActividad(actividadId);
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/respuestas/estudiante/{estudianteId}")
    public ResponseEntity<List<RespuestaEstudiante>> getRespuestasByEstudiante(@PathVariable Long estudianteId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorEstudiante(usuario.getId(), estudianteId);
        List<RespuestaEstudiante> respuestas = respuestaService.getRespuestasByEstudiante(estudianteId);
        return ResponseEntity.ok(respuestas);
    }

    @PutMapping("/respuestas/{id}/calificar")
    public ResponseEntity<RespuestaEstudiante> calificarRespuesta(
            @PathVariable Long id, @RequestParam Float nota,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorRespuesta(usuario.getId(), id);
        RespuestaEstudiante respuesta = respuestaService.calificarRespuesta(id, nota,
                usuario instanceof Profesor profesor ? profesor : null);
        return ResponseEntity.ok(respuesta);
    }

    // 6. Gestión de Estudiantes
    @GetMapping("/estudiantes/curso/{cursoId}")
    public ResponseEntity<List<Estudiante>> getEstudiantesByCurso(@PathVariable Long cursoId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorCurso(usuario.getId(), cursoId);
        List<Estudiante> estudiantes = estudianteService.getEstudiantesByCurso(cursoId);
        return ResponseEntity.ok(estudiantes);
    }

    @GetMapping(value = "/estudiantes/curso/{cursoId}/export", produces = "text/csv")
    public ResponseEntity<String> exportEstudiantesByCurso(@PathVariable Long cursoId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorCurso(usuario.getId(), cursoId);
        String csvData = estudianteService.exportEstudiantesByCursoToCsv(cursoId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=estudiantes_curso_" + cursoId + ".csv")
                .body(csvData);
    }

    @PutMapping("/estudiantes/{id}/estado")
    public ResponseEntity<Estudiante> cambiarEstadoEstudiante(
            @PathVariable Long id, @RequestParam String estado,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorEstudiante(usuario.getId(), id);
        Estudiante estudiante = estudianteService.cambiarEstadoEstudiante(id, estado);
        return ResponseEntity.ok(estudiante);
    }

    // 7. Reportes y Estadísticas
    @GetMapping("/reportes/promedios")
    public ResponseEntity<?> getPromediosByCursoMateria(
            @RequestParam Long cursoId,
            @RequestParam Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        return ResponseEntity.ok(respuestaService.getPromediosByCursoMateria(cursoId, materiaId));
    }

    @GetMapping("/reportes/entregas")
    public ResponseEntity<List<Object[]>> getEstadoEntregasByActividad(
            @RequestParam Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), actividadId);
        List<Object[]> entregas = respuestaService.getEstadoEntregasByActividad(actividadId);
        return ResponseEntity.ok(entregas);
    }

    @GetMapping("/actividades/{actividadId}/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticasActividad(
            @PathVariable Long actividadId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorActividad(usuario.getId(), actividadId);
        Map<String, Object> estadisticas = actividadService.getEstadisticasActividad(actividadId);
        return ResponseEntity.ok(estadisticas);
    }

    @GetMapping(value = "/reportes/promedios/export", produces = "application/pdf")
    public ResponseEntity<byte[]> exportPromediosToPdf(
            @RequestParam Long cursoId,
            @RequestParam Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {
        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        byte[] pdfReport = respuestaService.generatePromediosPdfReport(cursoId, materiaId);
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=promedios_curso_" + cursoId + "_materia_" + materiaId + ".pdf")
                .body(pdfReport);
    }

    // Nuevo endpoint para dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @AuthenticationPrincipal Usuario usuario) {
        Map<String, Object> dashboardData = actividadService.getDashboardData(usuario.getId());
        return ResponseEntity.ok(dashboardData);
    }

    // En ProfesorApi.java
    @GetMapping("/respuestas/filtrar")
    public ResponseEntity<List<RespuestaEstudiante>> filtrarRespuestas(
            @RequestParam(required = false) Long actividadId,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {

        // Validar acceso a los filtros
        if (materiaId != null)
            validacionService.validarProfesorMateria(usuario.getId(), materiaId);
        if (cursoId != null)
            validacionService.validarProfesorCurso(usuario.getId(), cursoId);
        if (actividadId != null)
            validacionService.validarProfesorActividad(usuario.getId(), actividadId);

        List<RespuestaEstudiante> respuestas = respuestaService.filtrarRespuestas(actividadId, cursoId, materiaId);
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping(value = "/reportes/resultados/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportResultados(
            @RequestParam Long cursoId,
            @RequestParam Long actividadId,
            @RequestParam String formato,
            @AuthenticationPrincipal Usuario usuario) {

        // Antes este endpoint no comprobaba la propiedad: cualquier profesor podía
        // descargar resultados de actividades ajenas.
        validacionService.validarProfesorActividad(usuario.getId(), actividadId);
        validacionService.validarProfesorCurso(usuario.getId(), cursoId);

        String extension = formato == null ? "" : formato.toLowerCase(java.util.Locale.ROOT);
        if (!FORMATOS_EXPORTACION.contains(extension)) {
            throw new com.rep.exception.ValidacionException(com.rep.exception.ErrorCode.FORMATO_EXPORTACION_NO_SOPORTADO,
                    "Formato no soportado. Use: " + FORMATOS_EXPORTACION);
        }
        byte[] reporte = respuestaService.generarReporteResultados(cursoId, actividadId, extension);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resultados." + extension)
                .body(reporte);
    }

    // 8. Gestión de Asistencia
    @GetMapping("/asistencia")
    public ResponseEntity<List<com.rep.model.Asistencia>> getAsistencia(
            @RequestParam Long cursoId,
            @RequestParam Long materiaId,
            @RequestParam String fecha,
            @AuthenticationPrincipal Usuario usuario) {

        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        List<com.rep.model.Asistencia> asistencia = asistenciaService.getAsistenciaByCursoMateriaFecha(
                usuario.getId(), cursoId, materiaId, java.time.LocalDate.parse(fecha));
        return ResponseEntity.ok(asistencia);
    }

    @PostMapping("/asistencia")
    public ResponseEntity<List<com.rep.model.Asistencia>> saveAsistencia(
            @RequestParam Long cursoId,
            @RequestParam Long materiaId,
            @RequestParam String fecha,
            @RequestBody List<com.rep.dto.asistencia.AsistenciaDTO> asistenciaDTOs,
            @AuthenticationPrincipal Usuario usuario) {

        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        List<com.rep.model.Asistencia> saved = asistenciaService.saveAsistencias(
                usuario.getId(), cursoId, materiaId, java.time.LocalDate.parse(fecha), asistenciaDTOs);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/asistencia/fechas")
    public ResponseEntity<List<java.time.LocalDate>> getFechasAsistencia(
            @RequestParam Long cursoId,
            @RequestParam Long materiaId,
            @AuthenticationPrincipal Usuario usuario) {

        validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
        List<java.time.LocalDate> fechas = asistenciaService.getFechasConAsistencia(usuario.getId(), cursoId,
                materiaId);
        return ResponseEntity.ok(fechas);
    }
}