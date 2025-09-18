package com.rep.service.impl;

import com.rep.model.Materia;
import com.rep.repositories.MateriaRepository;
import com.rep.service.logica.MateriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MateriaServiceImpl implements MateriaService {

    private final MateriaRepository materiaRepository;

    public MateriaServiceImpl(MateriaRepository materiaRepository) {
        this.materiaRepository = materiaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Materia> getMateriasByProfesor(Long profesorId) {
        return materiaRepository.findByProfesorId(profesorId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean profesorTieneAccesoAMateria(Long profesorId, Long materiaId) {
        return materiaRepository.existsByProfesorIdAndMateriaId(profesorId, materiaId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeMateria(Long materiaId) {
        return materiaRepository.existsById(materiaId);
    }
}