package com.rep.service.logica;

import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.exception.*;
import com.rep.model.*;
import com.rep.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gestión académica desde la administración: cursos, materias y asignaciones. */
@Service
@Transactional
public class AdminAcademicoService {
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final ProfesorRepository profesorRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;
    private final BloqueHorarioRepository bloqueHorarioRepository;

    public AdminAcademicoService(CursoRepository cursoRepository,
            MateriaRepository materiaRepository,
            ProfesorRepository profesorRepository,
            ProfesorMateriaRepository profesorMateriaRepository,
            EstudianteRepository estudianteRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoria,
            BloqueHorarioRepository bloqueHorarioRepository) {
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
        this.profesorRepository = profesorRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
        this.bloqueHorarioRepository = bloqueHorarioRepository;
    }

    // ------------------------------------------------------------------ cursos

    @Transactional(readOnly = true)
    public List<Curso> listarCursos() {
        return cursoRepository.findAllByOrderByGradoAsc();
    }

    @Transactional(readOnly = true)
    public Curso obtenerCurso(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.CURSO_NO_ENCONTRADO, id));
    }

    public Curso crearCurso(Integer grado, String grupo) {
        String grupoNormalizado = normalizarGrupo(grupo);
        validarGrado(grado);
        if (cursoRepository.existsByGradoAndGrupo(grado, grupoNormalizado)) {
            throw new ConflictoException(ErrorCode.CURSO_DUPLICADO, Map.of("grado", grado, "grupo", grupoNormalizado));
        }
        Curso curso = new Curso();
        curso.setGrado(grado);
        curso.setGrupo(grupoNormalizado);
        Curso guardado = cursoRepository.save(curso);
        auditoria.registrar("CREAR_CURSO", "ID: " + guardado.getId() + ", " + guardado.getNombre());
        return guardado;
    }

    public Curso actualizarCurso(Long id, Integer grado, String grupo) {
        Curso curso = obtenerCurso(id);
        String grupoNormalizado = normalizarGrupo(grupo);
        validarGrado(grado);
        if (cursoRepository.existsByGradoAndGrupoAndIdNot(grado, grupoNormalizado, id)) {
            throw new ConflictoException(ErrorCode.CURSO_DUPLICADO, Map.of("grado", grado, "grupo", grupoNormalizado));
        }
        curso.setGrado(grado);
        curso.setGrupo(grupoNormalizado);
        Curso guardado = cursoRepository.save(curso);
        auditoria.registrar("ACTUALIZAR_CURSO", "ID: " + guardado.getId() + ", " + guardado.getNombre());
        return guardado;
    }

    public void eliminarCurso(Long id) {
        Curso curso = obtenerCurso(id);
        if (estudianteRepository.existsByCursoId(id)) {
            throw new ConflictoException(ErrorCode.CURSO_CON_ESTUDIANTES);
        }
        if (profesorMateriaRepository.existsByCursoId(id)) {
            throw new ConflictoException(ErrorCode.CURSO_CON_ASIGNACIONES);
        }
        auditoria.registrar("ELIMINAR_CURSO", "ID: " + id + ", " + curso.getNombre());
        cursoRepository.delete(curso);
    }

    @Transactional(readOnly = true)
    public List<Estudiante> estudiantesDeCurso(Long cursoId) {
        obtenerCurso(cursoId);
        return estudianteRepository.findByCursoId(cursoId);
    }

    // ------------------------------------------------------------------ materias

    @Transactional(readOnly = true)
    public List<Materia> listarMaterias() {
        return materiaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Materia obtenerMateria(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.MATERIA_NO_ENCONTRADA, id));
    }

    public Materia crearMateria(String nombre) {
        String nombreNormalizado = normalizarNombreMateria(nombre);
        if (materiaRepository.findByNombre(nombreNormalizado).isPresent()) {
            throw new ConflictoException(ErrorCode.MATERIA_DUPLICADA, Map.of("nombre", nombreNormalizado));
        }
        Materia materia = new Materia();
        materia.setNombre(nombreNormalizado);
        Materia guardada = materiaRepository.save(materia);
        auditoria.registrar("CREAR_MATERIA", "ID: " + guardada.getId() + ", " + guardada.getNombre());
        return guardada;
    }

    public Materia actualizarMateria(Long id, String nombre) {
        Materia materia = obtenerMateria(id);
        String nombreNormalizado = normalizarNombreMateria(nombre);
        materiaRepository.findByNombre(nombreNormalizado)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ConflictoException(ErrorCode.MATERIA_DUPLICADA, Map.of("nombre", nombreNormalizado));
                });
        materia.setNombre(nombreNormalizado);
        Materia guardada = materiaRepository.save(materia);
        auditoria.registrar("ACTUALIZAR_MATERIA", "ID: " + id + ", " + guardada.getNombre());
        return guardada;
    }

    public void eliminarMateria(Long id) {
        obtenerMateria(id);
        if (profesorMateriaRepository.existsByMateriaId(id)) {
            throw new ConflictoException(ErrorCode.MATERIA_CON_ASIGNACIONES);
        }
        auditoria.registrar("ELIMINAR_MATERIA", "ID: " + id);
        materiaRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ asignaciones

    @Transactional(readOnly = true)
    public List<ProfesorMateria> listarAsignaciones(Long cursoId, Long materiaId) {
        if (cursoId != null && materiaId != null) {
            return profesorMateriaRepository.findByCursoIdAndMateriaId(cursoId, materiaId);
        }
        if (cursoId != null) {
            return profesorMateriaRepository.findByCursoId(cursoId);
        }
        if (materiaId != null) {
            return profesorMateriaRepository.findByMateriaId(materiaId);
        }
        return profesorMateriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProfesorMateria> asignacionesDeProfesor(Long profesorId) {
        return profesorMateriaRepository.findByProfesorId(profesorId);
    }

    public ProfesorMateria crearAsignacion(ProfesorMateriaRequest request) {
        if (request.getProfesorId() == null || request.getMateriaId() == null || request.getCursoId() == null) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "profesorId, materiaId y cursoId son obligatorios");
        }
        Profesor profesor = profesorRepository.findById(request.getProfesorId())
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.PROFESOR_NO_ENCONTRADO, request.getProfesorId()));
        Materia materia = obtenerMateria(request.getMateriaId());
        Curso curso = obtenerCurso(request.getCursoId());

        if (profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(
                profesor.getId(), materia.getId(), curso.getId())) {
            throw new ConflictoException(ErrorCode.ASIGNACION_DUPLICADA);
        }
        ProfesorMateria asignacion = new ProfesorMateria();
        asignacion.setProfesor(profesor);
        asignacion.setMateria(materia);
        asignacion.setCurso(curso);
        ProfesorMateria guardada = profesorMateriaRepository.save(asignacion);
        auditoria.registrar("CREAR_ASIGNACION", "Profesor: " + profesor.getId() + ", Materia: " + materia.getNombre()
                + ", Curso: " + curso.getNombre());
        return guardada;
    }

    public void eliminarAsignacion(Long id) {
        if (!profesorMateriaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException(ErrorCode.ASIGNACION_NO_ENCONTRADA, id);
        }
        auditoria.registrar("ELIMINAR_ASIGNACION", "ID: " + id);
        // Sus bloques del horario dejan de tener sentido sin la asignación
        bloqueHorarioRepository.deleteByAsignacion(id);
        profesorMateriaRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ estadísticas

    @Transactional(readOnly = true)
    public Map<String, Long> estadisticas() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("usuarios", usuarioRepository.count());
        stats.put("estudiantes", estudianteRepository.count());
        stats.put("profesores", profesorRepository.count());
        stats.put("cursos", cursoRepository.count());
        stats.put("materias", materiaRepository.count());
        return stats;
    }

    // ------------------------------------------------------------------ helpers

    private static void validarGrado(Integer grado) {
        if (grado == null || grado < 1 || grado > 12) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "El grado debe estar entre 1 y 12",
                    Map.of("grado", "Debe estar entre 1 y 12"));
        }
    }

    private static String normalizarGrupo(String grupo) {
        if (grupo == null || grupo.isBlank() || grupo.trim().length() > 5) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "El grupo es obligatorio (máximo 5 caracteres)",
                    Map.of("grupo", "Obligatorio, máximo 5 caracteres"));
        }
        return grupo.trim().toUpperCase();
    }

    private static String normalizarNombreMateria(String nombre) {
        if (nombre == null || nombre.trim().length() < 3 || nombre.trim().length() > 100) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "El nombre de la materia debe tener entre 3 y 100 caracteres",
                    Map.of("nombre", "Entre 3 y 100 caracteres"));
        }
        return nombre.trim();
    }
}
