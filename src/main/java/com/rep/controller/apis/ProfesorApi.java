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
    @Autowired
    private ProfesorMateriaRepository profesorMateriaRepository;
    @Autowired
    private ProfesorMateriaService profesorMateriaService;
    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private MateriaService materiaService;

    @Autowired
    private CursoService cursoService;

    @Autowired
    private ActividadService actividadService;

    @Autowired
    private PreguntaService preguntaService;

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private RespuestaService respuestaService;

    @Autowired
    private ValidacionService validacionService;

    // 1. Gestión de Materias
    @GetMapping("/materias")
    public ResponseEntity<?> getMateriasAsignadas(@AuthenticationPrincipal Usuario usuario) {
        try {
            List<Materia> materias = materiaService.getMateriasByProfesor(usuario.getId());
            return ResponseEntity.ok(materias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener materias: " + e.getMessage());
        }
    }

    // 2. Gestión de Cursos/Grupos
    // En ProfesorApi.java
    @GetMapping("/cursos")
    public ResponseEntity<List<CursoDTO>> getCursosByProfesor(@AuthenticationPrincipal Usuario usuario) {
        List<CursoDTO> cursos = cursoService.getCursosByProfesor(usuario.getId());
        return ResponseEntity.ok(cursos);
    }



//    // 4. Gestión de Preguntas
//    @PostMapping("/preguntas")
//    public ResponseEntity<Pregunta> crearPregunta(@RequestBody Pregunta pregunta,
//                                                  @AuthenticationPrincipal Usuario usuario) {
//        validacionService.validarProfesorActividad(usuario.getId(), pregunta.getActividad().getId());
//        Pregunta nuevaPregunta = preguntaService.crearPregunta(pregunta);
//        return ResponseEntity.ok(nuevaPregunta);
//    }
//
//    @PutMapping("/preguntas/{id}")
//    public ResponseEntity<Pregunta> editarPregunta(@PathVariable Long id,
//                                                   @RequestBody Pregunta pregunta,
//                                                   @AuthenticationPrincipal Usuario usuario) {
//        validacionService.validarProfesorPregunta(usuario.getId(), id);
//        Pregunta preguntaActualizada = preguntaService.editarPregunta(id, pregunta);
//        return ResponseEntity.ok(preguntaActualizada);
//    }
//
//    @DeleteMapping("/preguntas/{id}")
//    public ResponseEntity<Void> eliminarPregunta(@PathVariable Long id,
//                                                 @AuthenticationPrincipal Usuario usuario) {
//        validacionService.validarProfesorPregunta(usuario.getId(), id);
//        preguntaService.eliminarPregunta(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PostMapping("/opciones")
//    public ResponseEntity<Opcion> crearOpcion(@RequestBody Opcion opcion,
//                                              @AuthenticationPrincipal Usuario usuario) {
//        validacionService.validarProfesorOpcion(usuario.getId(), opcion.getPregunta().getId());
//        Opcion nuevaOpcion = preguntaService.crearOpcion(opcion);
//        return ResponseEntity.ok(nuevaOpcion);
//    }

    // 5. Revisión de Resultados
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
        RespuestaEstudiante respuesta = respuestaService.calificarRespuesta(id, nota);
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
        try {
            validacionService.validarProfesorCursoMateria(usuario.getId(), cursoId, materiaId);
            List<Object[]> promedios = respuestaService.getPromediosByCursoMateria(cursoId, materiaId);
            return ResponseEntity.ok(promedios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
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
                .header("Content-Disposition", "attachment; filename=promedios_curso_" + cursoId + "_materia_" + materiaId + ".pdf")
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
        if (materiaId != null) validacionService.validarProfesorMateria(usuario.getId(), materiaId);
        if (cursoId != null) validacionService.validarProfesorCurso(usuario.getId(), cursoId);
        if (actividadId != null) validacionService.validarProfesorActividad(usuario.getId(), actividadId);

        List<RespuestaEstudiante> respuestas = respuestaService.filtrarRespuestas(actividadId, cursoId, materiaId);
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping(value = "/reportes/resultados/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportResultados(
            @RequestParam Long cursoId,
            @RequestParam Long actividadId,
            @RequestParam String formato,
            @RequestHeader("Authorization") String authHeader) {  // Token en header

        // Validar token y permisos...
        byte[] reporte = respuestaService.generarReporteResultados(cursoId, actividadId, formato);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resultados." + formato)
                .body(reporte);
    }
}