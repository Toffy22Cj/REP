package com.rep.service.logica;

import com.rep.dto.notas.PeriodoDTO;
import com.rep.exception.ConflictoException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.model.PeriodoAcademico;
import com.rep.repositories.PeriodoAcademicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Periodos académicos del año lectivo. Reglas: inicio ≤ fin, sin cruces de fechas ni
 * números repetidos dentro del año, y suma de porcentajes del año ≤ 100.
 */
@Service
@Transactional
public class PeriodoService {
    private final PeriodoAcademicoRepository repositorio;
    private final AuditoriaService auditoria;

    public PeriodoService(PeriodoAcademicoRepository repositorio, AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listar(Integer anio) {
        return anio == null ? repositorio.findAllByOrderByAnioDescNumeroAsc() : repositorio.findByAnioOrderByNumeroAsc(anio);
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.PERIODO_NO_ENCONTRADO, id));
    }

    /**
     * Periodos del año lectivo vigente: el que contiene la fecha dada o, si ninguno la
     * contiene (vacaciones), el último año configurado. Vacío si no hay periodos.
     */
    @Transactional(readOnly = true)
    public List<PeriodoAcademico> delAnioVigente(LocalDate hoy) {
        List<PeriodoAcademico> todos = repositorio.findAllByOrderByAnioDescNumeroAsc();
        Integer anio = todos.stream().filter(p -> p.contiene(hoy)).map(PeriodoAcademico::getAnio).findFirst()
                .orElseGet(() -> todos.stream().filter(p -> !p.getFechaInicio().isAfter(hoy))
                        .map(PeriodoAcademico::getAnio).findFirst()
                        .orElse(todos.isEmpty() ? null : todos.get(0).getAnio()));
        return anio == null ? List.of() : repositorio.findByAnioOrderByNumeroAsc(anio);
    }

    public PeriodoAcademico crear(PeriodoDTO dto) {
        PeriodoAcademico p = new PeriodoAcademico();
        aplicar(p, dto);
        validar(p);
        PeriodoAcademico guardado = repositorio.save(p);
        auditoria.registrar("CREAR_PERIODO", guardado.getAnio() + " · " + guardado.getNombre());
        return guardado;
    }

    public PeriodoAcademico actualizar(Long id, PeriodoDTO dto) {
        PeriodoAcademico p = obtener(id);
        aplicar(p, dto);
        validar(p);
        auditoria.registrar("ACTUALIZAR_PERIODO", p.getAnio() + " · " + p.getNombre());
        return repositorio.save(p);
    }

    public void eliminar(Long id) {
        PeriodoAcademico p = obtener(id);
        repositorio.delete(p);
        auditoria.registrar("ELIMINAR_PERIODO", p.getAnio() + " · " + p.getNombre());
    }

    private static void aplicar(PeriodoAcademico p, PeriodoDTO dto) {
        p.setAnio(dto.anio());
        p.setNumero(dto.numero());
        p.setNombre(dto.nombre().trim());
        p.setFechaInicio(dto.fechaInicio());
        p.setFechaFin(dto.fechaFin());
        p.setPorcentaje(dto.porcentaje());
    }

    private void validar(PeriodoAcademico p) {
        if (p.getFechaInicio().isAfter(p.getFechaFin())) {
            throw new ValidacionException(ErrorCode.PERIODO_FECHAS_INVALIDAS, ErrorCode.PERIODO_FECHAS_INVALIDAS.getDefaultMessage(),
                    Map.of("fechaFin", "Debe ser posterior a la fecha de inicio"));
        }
        List<PeriodoAcademico> otros = repositorio.findByAnioOrderByNumeroAsc(p.getAnio()).stream()
                .filter(o -> !Objects.equals(o.getId(), p.getId())).toList();
        for (PeriodoAcademico o : otros) {
            if (o.getNumero().equals(p.getNumero())) {
                throw new ConflictoException(ErrorCode.PERIODO_DUPLICADO, Map.of("numero", p.getNumero()));
            }
            if (!p.getFechaInicio().isAfter(o.getFechaFin()) && !o.getFechaInicio().isAfter(p.getFechaFin())) {
                throw new ConflictoException(ErrorCode.PERIODO_SOLAPADO,
                        "Las fechas se cruzan con «" + o.getNombre() + "» (" + o.getFechaInicio() + " a " + o.getFechaFin() + ")");
            }
        }
        int suma = otros.stream().mapToInt(PeriodoAcademico::getPorcentaje).sum() + p.getPorcentaje();
        if (suma > 100) {
            throw new ValidacionException(ErrorCode.PERIODO_PORCENTAJE_EXCEDIDO,
                    "Con este periodo el año suma " + suma + " %; el máximo es 100 %",
                    Map.of("porcentaje", "Disponible: " + (100 - (suma - p.getPorcentaje())) + " %"));
        }
    }
}
