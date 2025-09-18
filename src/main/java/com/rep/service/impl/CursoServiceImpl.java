package com.rep.service.impl;

import com.rep.model.Curso;
import com.rep.model.ProfesorMateria;
import com.rep.repositories.CursoRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.dto.curso.CursoDTO;
import com.rep.service.logica.CursoService;
import com.rep.service.mapas.CursoMapper;
import com.rep.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursoRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final CursoMapper cursoMapper;

    public CursoServiceImpl(CursoRepository cursoRepository,
                            ProfesorMateriaRepository profesorMateriaRepository,
                            CursoMapper cursoMapper) {
        this.cursoRepository = cursoRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.cursoMapper = cursoMapper;
    }

    @Override
    public CursoDTO crearCurso(CursoDTO cursoDTO) {
        if (cursoRepository.existsByGradoAndGrupo(cursoDTO.getGrado(), cursoDTO.getGrupo())) {
            throw new IllegalStateException("Ya existe un curso con este grado y grupo");
        }
        Curso curso = cursoMapper.toEntity(cursoDTO);
        return cursoMapper.toDto(cursoRepository.save(curso));
    }

    @Override
    public CursoDTO actualizarCurso(Long id, CursoDTO cursoDTO) {
        Curso cursoExistente = cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado"));

        if (cursoRepository.existsByGradoAndGrupoAndIdNot(cursoDTO.getGrado(), cursoDTO.getGrupo(), id)) {
            throw new IllegalStateException("Ya existe otro curso con este grado y grupo");
        }

        // Actualiza solo los campos permitidos
        cursoMapper.updateCursoFromDto(cursoDTO, cursoExistente);
        return cursoMapper.toDto(cursoRepository.save(cursoExistente));
    }

    @Override
    public void eliminarCurso(Long id) {
        if (!cursoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Curso no encontrado");
        }

        if (profesorMateriaRepository.existsByCursoId(id)) {
            throw new IllegalStateException("No se puede eliminar el curso porque tiene materias asignadas");
        }

        cursoRepository.deleteById(id);
    }

    @Override
    public CursoDTO obtenerCursoPorId(Long id) {
        return cursoMapper.toDto(cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado")));
    }

    @Override
    public List<CursoDTO> listarTodosLosCursos() {
        return cursoRepository.findAllByOrderByGradoAsc().stream()
                .map(cursoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CursoDTO> contarEstudiantesPorCurso() {
        return cursoRepository.contarEstudiantesPorCurso().stream()
                .map(arr -> {
                    CursoDTO dto = new CursoDTO();
                    dto.setId((Long) arr[0]);
                    dto.setGrado((Integer) arr[1]);
                    dto.setGrupo((String) arr[2]);
                    dto.setCantidadEstudiantes(((Number) arr[3]).longValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CursoDTO> getCursosByProfesor(Long profesorId) {
        return profesorMateriaRepository.findByProfesorId(profesorId).stream()
                .map(ProfesorMateria::getCurso)
                .distinct()
                .map(cursoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean profesorTieneAccesoACurso(Long profesorId, Long cursoId) {
        return profesorMateriaRepository.existsByProfesorIdAndCursoId(profesorId, cursoId);
    }

    @Override
    public boolean existeCurso(Long cursoId) {
        return cursoRepository.existsById(cursoId);
    }
}