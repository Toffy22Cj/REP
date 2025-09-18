package com.rep.service.impl;

import com.rep.model.ProfesorMateria;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.service.logica.ProfesorMateriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfesorMateriaServiceImpl implements ProfesorMateriaService {

    private final ProfesorMateriaRepository profesorMateriaRepository;

    @Autowired
    public ProfesorMateriaServiceImpl(ProfesorMateriaRepository profesorMateriaRepository) {
        this.profesorMateriaRepository = profesorMateriaRepository;
    }

    @Override
    public ProfesorMateria guardarProfesorMateria(ProfesorMateria profesorMateria) {
        return profesorMateriaRepository.save(profesorMateria);
    }

    @Override
    public Optional<ProfesorMateria> findByProfesorIdAndMateriaIdAndCursoId(Long profesorId, Long materiaId, Long cursoId) {
        return profesorMateriaRepository.findByProfesorIdAndMateriaIdAndCursoId(profesorId, materiaId, cursoId);
    }

    @Override
    public List<ProfesorMateria> findByProfesorId(Long profesorId) {
        return profesorMateriaRepository.findByProfesorId(profesorId);
    }

    @Override
    public List<ProfesorMateria> findByMateriaId(Long materiaId) {
        return profesorMateriaRepository.findByMateriaId(materiaId);
    }

    @Override
    public List<ProfesorMateria> findByCursoId(Long cursoId) {
        return profesorMateriaRepository.findByCursoId(cursoId);
    }

    @Override
    public boolean existsByProfesorIdAndMateriaIdAndCursoId(Long profesorId, Long materiaId, Long cursoId) {
        return profesorMateriaRepository.existsByProfesorIdAndMateriaIdAndCursoId(profesorId, materiaId, cursoId);
    }

    @Override
    public boolean existsByProfesorIdAndCursoId(Long profesorId, Long cursoId) {
        return profesorMateriaRepository.existsByProfesorIdAndCursoId(profesorId, cursoId);
    }

    @Override
    public long countDistinctCursoByProfesorId(Long profesorId) {
        return profesorMateriaRepository.countDistinctCursoByProfesorId(profesorId);
    }

    @Override
    public List<ProfesorMateria> findByCursoIdAndMateriaId(Long cursoId, Long materiaId) {
        return profesorMateriaRepository.findByCursoIdAndMateriaId(cursoId, materiaId);
    }
}