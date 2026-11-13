package com.rep.service.logica;

import com.rep.dto.notas.NotasDTOs.*;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.model.*;
import com.rep.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reporte de notas.
 *
 * <p>Reglas (configurables en {@code application.properties}):</p>
 * <ul>
 *   <li>Solo cuentan las actividades activas.</li>
 *   <li>Promedio = media de las notas entregadas; si
 *       {@code app.notas.vencidas-cuentan-cero=true} (por defecto) las actividades
 *       vencidas sin entrega cuentan como 0.0. Las pendientes o en curso no cuentan.</li>
 *   <li>Aprobada si el promedio ≥ {@code app.notas.minima-aprobatoria} (3.0 por defecto).</li>
 *   <li>Las entregas con preguntas abiertas aún no revisadas muestran nota preliminar
 *       (estado ENTREGADA).</li>
 * </ul>
 *
 * <p>Cada reporte se calcula con un número fijo de consultas (sin N+1), independiente
 * del número de estudiantes o actividades.</p>
 */
@Service
@Transactional(readOnly = true)
public class NotasService {
    private final EstudianteRepository estudianteRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final ActividadRepository actividadRepository;
    private final RespuestaEstudianteRepository respuestaRepository;
    private final PreguntaRepository preguntaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final float notaMinima;
    private final boolean vencidasCuentanCero;

    public NotasService(EstudianteRepository estudianteRepository, CursoRepository cursoRepository,
            MateriaRepository materiaRepository, ProfesorMateriaRepository profesorMateriaRepository,
            ActividadRepository actividadRepository, RespuestaEstudianteRepository respuestaRepository,
            PreguntaRepository preguntaRepository, AsistenciaRepository asistenciaRepository,
            @Value("${app.notas.minima-aprobatoria:3.0}") float notaMinima,
            @Value("${app.notas.vencidas-cuentan-cero:true}") boolean vencidasCuentanCero) {
        this.estudianteRepository = estudianteRepository;
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.actividadRepository = actividadRepository;
        this.respuestaRepository = respuestaRepository;
        this.preguntaRepository = preguntaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.notaMinima = notaMinima;
        this.vencidasCuentanCero = vencidasCuentanCero;
    }

    // ================================================================== boletín del estudiante

    public Boletin boletin(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ESTUDIANTE_NO_ENCONTRADO, estudianteId));
        Curso curso = estudiante.getCurso();
        if (curso == null) {
            throw new ValidacionException(ErrorCode.ESTUDIANTE_SIN_CURSO);
        }

        List<ProfesorMateria> asignaciones = profesorMateriaRepository.findByCursoId(curso.getId());
        List<Actividad> actividades = activas(actividadRepository.findByProfesorMateriaCursoId(curso.getId()));
        Map<Long, RespuestaEstudiante> respuestas = respuestaRepository.findByEstudianteId(estudianteId).stream()
                .collect(Collectors.toMap(r -> r.getActividad().getId(), Function.identity(), (a, b) -> a));
        Set<Long> conAbiertas = conPreguntasAbiertas(actividades);
        Map<String, ResumenAsistencia> asistencia = asistenciaPorEstudianteYMateria(curso.getId());
        LocalDateTime ahora = LocalDateTime.now();

        // Una entrada por materia (si hay varios profesores en la misma materia se agrupan)
        Map<Long, List<ProfesorMateria>> porMateria = asignaciones.stream()
                .collect(Collectors.groupingBy(pm -> pm.getMateria().getId(), TreeMap::new, Collectors.toList()));

        List<NotasMateria> materias = new ArrayList<>();
        for (List<ProfesorMateria> pms : porMateria.values()) {
            Materia materia = pms.get(0).getMateria();
            Set<Long> pmIds = pms.stream().map(ProfesorMateria::getId).collect(Collectors.toSet());
            List<NotaActividad> notas = actividades.stream()
                    .filter(a -> pmIds.contains(a.getProfesorMateria().getId()))
                    .sorted(Comparator.comparing(Actividad::getFechaEntrega, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(a -> {
                        RespuestaEstudiante r = respuestas.get(a.getId());
                        EstadoNota estado = estado(a, r, conAbiertas.contains(a.getId()), ahora);
                        return new NotaActividad(a.getId(), a.getTitulo(), a.getTipo().name(), a.getFechaEntrega(),
                                estado, notaVisible(estado, r), r != null ? r.getObservaciones() : null);
                    })
                    .toList();

            Float promedio = promedio(notas.stream().map(n -> notaComputable(n.estado(), n.nota())).toList());
            String profesor = pms.stream().map(pm -> nombre(pm.getProfesor())).distinct().collect(Collectors.joining(", "));
            int calificadas = (int) notas.stream().filter(n -> n.estado() == EstadoNota.CALIFICADA || n.estado() == EstadoNota.ENTREGADA).count();
            int pendientes = (int) notas.stream().filter(n -> n.estado() == EstadoNota.PENDIENTE || n.estado() == EstadoNota.EN_CURSO).count();
            materias.add(new NotasMateria(materia.getId(), materia.getNombre(), profesor, promedio,
                    EscalaCalificacion.desempeno(promedio, notaMinima), promedio == null ? null : promedio >= notaMinima,
                    calificadas, pendientes,
                    asistencia.getOrDefault(estudianteId + ":" + materia.getId(), ResumenAsistencia.VACIO), notas));
        }
        materias.sort(Comparator.comparing(NotasMateria::materia));

        Float general = promedio(materias.stream().map(NotasMateria::promedio).toList());
        int aprobadas = (int) materias.stream().filter(m -> Boolean.TRUE.equals(m.aprobada())).count();
        int reprobadas = (int) materias.stream().filter(m -> Boolean.FALSE.equals(m.aprobada())).count();
        return new Boletin(estudiante.getId(), nombre(estudiante), estudiante.getIdentificacion(), curso.getNombre(),
                ahora, notaMinima, general, EscalaCalificacion.desempeno(general, notaMinima), aprobadas, reprobadas,
                materias);
    }

    // ================================================================== planilla del profesor

    public Planilla planilla(Long cursoId, Long materiaId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.CURSO_NO_ENCONTRADO, cursoId));
        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.MATERIA_NO_ENCONTRADA, materiaId));

        List<Actividad> actividades = activas(
                actividadRepository.findByProfesorMateriaCursoIdAndProfesorMateriaMateriaId(cursoId, materiaId)).stream()
                .sorted(Comparator.comparing(Actividad::getFechaEntrega, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Actividad::getId))
                .toList();
        List<Estudiante> estudiantes = estudianteRepository.findByCursoId(cursoId).stream()
                .sorted(Comparator.comparing(Estudiante::getApellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Estudiante::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<Long> actividadIds = actividades.stream().map(Actividad::getId).toList();

        // (estudianteId, actividadId) -> respuesta
        Map<String, RespuestaEstudiante> respuestas = actividadIds.isEmpty() ? Map.of()
                : respuestaRepository.findByActividadIdIn(actividadIds).stream()
                        .collect(Collectors.toMap(r -> r.getEstudiante().getId() + ":" + r.getActividad().getId(),
                                Function.identity(), (a, b) -> a));
        Set<Long> conAbiertas = conPreguntasAbiertas(actividades);
        Map<String, ResumenAsistencia> asistencia = asistenciaPorEstudianteYMateria(cursoId);
        LocalDateTime ahora = LocalDateTime.now();

        List<FilaEstudiante> filas = new ArrayList<>();
        Map<Long, List<Float>> notasPorActividad = new HashMap<>();
        Map<Long, Integer> entregasPorActividad = new HashMap<>();
        for (Estudiante e : estudiantes) {
            List<CeldaNota> celdas = new ArrayList<>();
            List<Float> computables = new ArrayList<>();
            for (Actividad a : actividades) {
                RespuestaEstudiante r = respuestas.get(e.getId() + ":" + a.getId());
                EstadoNota estado = estado(a, r, conAbiertas.contains(a.getId()), ahora);
                Float nota = notaVisible(estado, r);
                celdas.add(new CeldaNota(a.getId(), estado, nota));
                Float computable = notaComputable(estado, nota);
                computables.add(computable);
                if (estado == EstadoNota.CALIFICADA || estado == EstadoNota.ENTREGADA) {
                    notasPorActividad.computeIfAbsent(a.getId(), k -> new ArrayList<>()).add(nota);
                    entregasPorActividad.merge(a.getId(), 1, Integer::sum);
                }
            }
            Float prom = promedio(computables);
            filas.add(new FilaEstudiante(e.getId(), nombre(e), e.getIdentificacion(), celdas, prom,
                    EscalaCalificacion.desempeno(prom, notaMinima), prom == null ? null : prom >= notaMinima,
                    asistencia.getOrDefault(e.getId() + ":" + materiaId, ResumenAsistencia.VACIO).ausencias()));
        }

        List<ColumnaActividad> columnas = actividades.stream()
                .map(a -> new ColumnaActividad(a.getId(), a.getTitulo(), a.getTipo().name(), a.getFechaEntrega(),
                        promedio(notasPorActividad.getOrDefault(a.getId(), List.of())),
                        entregasPorActividad.getOrDefault(a.getId(), 0)))
                .toList();
        Float promedioCurso = promedio(filas.stream().map(FilaEstudiante::promedio).toList());
        int aprobados = (int) filas.stream().filter(f -> Boolean.TRUE.equals(f.aprobada())).count();
        int reprobados = (int) filas.stream().filter(f -> Boolean.FALSE.equals(f.aprobada())).count();
        return new Planilla(cursoId, curso.getNombre(), materiaId, materia.getNombre(), ahora, notaMinima, columnas,
                filas, promedioCurso, aprobados, reprobados);
    }

    // ================================================================== reglas

    static EstadoNota estado(Actividad a, RespuestaEstudiante r, boolean tieneAbiertas, LocalDateTime ahora) {
        if (r != null && Boolean.TRUE.equals(r.getEntregado())) {
            return tieneAbiertas && r.getCalificadoPor() == null ? EstadoNota.ENTREGADA : EstadoNota.CALIFICADA;
        }
        if (a.getFechaHoraEntrega() != null && ahora.isAfter(a.getFechaHoraEntrega())) {
            return EstadoNota.VENCIDA;
        }
        return r != null ? EstadoNota.EN_CURSO : EstadoNota.PENDIENTE;
    }

    private static Float notaVisible(EstadoNota estado, RespuestaEstudiante r) {
        return (estado == EstadoNota.CALIFICADA || estado == EstadoNota.ENTREGADA) && r != null ? r.getNota() : null;
    }

    /** Nota que entra en el promedio (null = no cuenta). */
    private Float notaComputable(EstadoNota estado, Float nota) {
        if (estado == EstadoNota.VENCIDA) {
            return vencidasCuentanCero ? 0f : null;
        }
        return nota;
    }

    private static Float promedio(List<Float> valores) {
        DoubleSummaryStatistics stats = valores.stream().filter(Objects::nonNull)
                .mapToDouble(Float::doubleValue).summaryStatistics();
        return stats.getCount() == 0 ? null : EscalaCalificacion.redondear(stats.getAverage());
    }

    private static List<Actividad> activas(List<Actividad> actividades) {
        return actividades.stream().filter(a -> !Boolean.FALSE.equals(a.getActiva())).toList();
    }

    private Set<Long> conPreguntasAbiertas(List<Actividad> actividades) {
        if (actividades.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(preguntaRepository.findActividadIdsConTipo(
                actividades.stream().map(Actividad::getId).toList(), Pregunta.TipoPregunta.RESPUESTA_ABIERTA));
    }

    /** Clave "estudianteId:materiaId" → resumen de asistencia. */
    private Map<String, ResumenAsistencia> asistenciaPorEstudianteYMateria(Long cursoId) {
        Map<String, long[]> conteos = new HashMap<>();
        for (Object[] fila : asistenciaRepository.contarPorCurso(cursoId)) {
            String clave = fila[0] + ":" + fila[1];
            long[] c = conteos.computeIfAbsent(clave, k -> new long[3]);
            long n = ((Number) fila[3]).longValue();
            switch ((Asistencia.EstadoAsistencia) fila[2]) {
                case PRESENTE -> c[0] += n;
                case NO_INGRESO -> c[1] += n;
                case EXCUSA -> c[2] += n;
            }
        }
        Map<String, ResumenAsistencia> resultado = new HashMap<>();
        conteos.forEach((k, c) -> resultado.put(k, new ResumenAsistencia(c[0], c[1], c[2])));
        return resultado;
    }

    private static String nombre(Usuario u) {
        return (u.getNombre() + " " + (u.getApellido() == null ? "" : u.getApellido())).trim();
    }
}
