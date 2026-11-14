package com.rep.horario;

import com.rep.exception.ConflictoException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.horario.HorarioDTOs.Bloque;
import com.rep.horario.HorarioDTOs.GuardarBloque;
import com.rep.model.BloqueHorario;
import com.rep.model.Estudiante;
import com.rep.model.ProfesorMateria;
import com.rep.repositories.BloqueHorarioRepository;
import com.rep.repositories.EstudianteRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.service.logica.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Horario de clases. Reglas: inicio anterior al fin; un curso no tiene dos clases a la
 * vez; un profesor no está en dos clases a la vez.
 */
@Service
@Transactional
public class HorarioService {
    private static final Comparator<BloqueHorario> ORDEN = Comparator.comparing(BloqueHorario::getDia)
            .thenComparing(BloqueHorario::getHoraInicio);

    private final BloqueHorarioRepository bloques;
    private final ProfesorMateriaRepository asignaciones;
    private final EstudianteRepository estudiantes;
    private final AuditoriaService auditoria;

    public HorarioService(BloqueHorarioRepository bloques, ProfesorMateriaRepository asignaciones,
            EstudianteRepository estudiantes, AuditoriaService auditoria) {
        this.bloques = bloques;
        this.asignaciones = asignaciones;
        this.estudiantes = estudiantes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Bloque> listar(Long cursoId, Long profesorId) {
        List<BloqueHorario> lista = cursoId != null ? bloques.findByProfesorMateriaCursoId(cursoId)
                : profesorId != null ? bloques.findByProfesorMateriaProfesorId(profesorId)
                : bloques.findAll();
        return lista.stream()
                .filter(b -> profesorId == null || b.getProfesorMateria().getProfesor().getId().equals(profesorId))
                .sorted(ORDEN).map(Bloque::de).toList();
    }

    @Transactional(readOnly = true)
    public List<Bloque> deEstudiante(Long estudianteId) {
        Estudiante e = estudiantes.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ESTUDIANTE_NO_ENCONTRADO, estudianteId));
        if (e.getCurso() == null) {
            return List.of();
        }
        return listar(e.getCurso().getId(), null);
    }

    @Transactional(readOnly = true)
    public List<Bloque> deProfesor(Long profesorId) {
        return listar(null, profesorId);
    }

    public Bloque crear(GuardarBloque dto) {
        BloqueHorario b = new BloqueHorario();
        aplicar(b, dto);
        validar(b);
        BloqueHorario guardado = bloques.save(b);
        auditoria.registrar("CREAR_BLOQUE_HORARIO", descripcion(guardado));
        return Bloque.de(guardado);
    }

    public Bloque actualizar(Long id, GuardarBloque dto) {
        BloqueHorario b = obtener(id);
        aplicar(b, dto);
        validar(b);
        auditoria.registrar("ACTUALIZAR_BLOQUE_HORARIO", descripcion(b));
        return Bloque.de(bloques.save(b));
    }

    public void eliminar(Long id) {
        BloqueHorario b = obtener(id);
        bloques.delete(b);
        auditoria.registrar("ELIMINAR_BLOQUE_HORARIO", descripcion(b));
    }

    private BloqueHorario obtener(Long id) {
        return bloques.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.HORARIO_BLOQUE_NO_ENCONTRADO, id));
    }

    private void aplicar(BloqueHorario b, GuardarBloque dto) {
        if (dto.dia() < 1 || dto.dia() > 7) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "Día no válido",
                    Map.of("dia", "Use 1 (lunes) a 7 (domingo)"));
        }
        ProfesorMateria pm = asignaciones.findById(dto.asignacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ASIGNACION_NO_ENCONTRADA, dto.asignacionId()));
        b.setProfesorMateria(pm);
        b.setDia(dto.diaSemana());
        b.setHoraInicio(dto.horaInicio());
        b.setHoraFin(dto.horaFin());
        b.setAula(dto.aula() == null || dto.aula().isBlank() ? null : dto.aula().trim());
    }

    private void validar(BloqueHorario b) {
        if (!b.getHoraInicio().isBefore(b.getHoraFin())) {
            throw new ValidacionException(ErrorCode.HORARIO_HORAS_INVALIDAS, ErrorCode.HORARIO_HORAS_INVALIDAS.getDefaultMessage(),
                    Map.of("horaFin", "Debe ser posterior a la hora de inicio"));
        }
        ProfesorMateria pm = b.getProfesorMateria();
        for (BloqueHorario otro : bloques.candidatosACruce(b.getDia(), pm.getCurso().getId(), pm.getProfesor().getId())) {
            if (Objects.equals(otro.getId(), b.getId()) || !b.seCruzaCon(otro)) continue;
            var opm = otro.getProfesorMateria();
            String franja = otro.getHoraInicio() + "–" + otro.getHoraFin();
            if (opm.getCurso().getId().equals(pm.getCurso().getId())) {
                throw new ConflictoException(ErrorCode.HORARIO_CRUCE_CURSO, "El curso " + pm.getCurso().getNombre()
                        + " ya tiene " + opm.getMateria().getNombre() + " de " + franja);
            }
            throw new ConflictoException(ErrorCode.HORARIO_CRUCE_PROFESOR, "El profesor ya tiene "
                    + opm.getMateria().getNombre() + " en " + opm.getCurso().getNombre() + " de " + franja);
        }
    }

    private static String descripcion(BloqueHorario b) {
        var pm = b.getProfesorMateria();
        return pm.getMateria().getNombre() + " · " + pm.getCurso().getNombre() + " · " + b.getDia() + " "
                + b.getHoraInicio() + "-" + b.getHoraFin();
    }
}
