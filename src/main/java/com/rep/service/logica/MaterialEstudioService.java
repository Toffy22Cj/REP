package com.rep.service.logica;

import com.rep.dto.material.MaterialDTO;
import com.rep.exception.AccessDeniedException;
import com.rep.exception.ErrorCode;
import com.rep.exception.RecursoNoEncontradoException;
import com.rep.exception.ValidacionException;
import com.rep.model.*;
import com.rep.repositories.EstudianteRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.RecursoRepository;
import com.rep.service.archivos.AlmacenamientoArchivosService;
import com.rep.service.archivos.AlmacenamientoArchivosService.ArchivoGuardado;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Material de estudio por materia y curso.
 *
 * <ul>
 *   <li>Profesor: publica y elimina material de sus asignaciones; ve el suyo.</li>
 *   <li>Estudiante: ve y descarga el material de su curso.</li>
 *   <li>Administrador: ve y elimina cualquier material.</li>
 * </ul>
 */
@Service
@Transactional
public class MaterialEstudioService {
    private static final String CARPETA = "material";

    private final RecursoRepository recursoRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final EstudianteRepository estudianteRepository;
    private final AlmacenamientoArchivosService almacenamiento;

    public MaterialEstudioService(RecursoRepository recursoRepository,
            ProfesorMateriaRepository profesorMateriaRepository,
            EstudianteRepository estudianteRepository,
            AlmacenamientoArchivosService almacenamiento) {
        this.recursoRepository = recursoRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.estudianteRepository = estudianteRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public List<MaterialDTO> listar(Usuario usuario, Long cursoId, Long materiaId) {
        List<Recurso> recursos = switch (usuario.getRol()) {
            case ADMIN -> recursoRepository.buscar(cursoId, materiaId, null);
            case PROFESOR -> recursoRepository.buscar(cursoId, materiaId, usuario.getId());
            case ESTUDIANTE -> {
                Long cursoEstudiante = cursoDelEstudiante(usuario.getId());
                if (cursoId != null && !cursoId.equals(cursoEstudiante)) {
                    throw new AccessDeniedException("Solo puede ver el material de su curso");
                }
                yield recursoRepository.buscar(cursoEstudiante, materiaId, null);
            }
        };
        return recursos.stream().map(MaterialDTO::from).toList();
    }

    public MaterialDTO publicar(Usuario profesor, Long materiaId, Long cursoId, String titulo, String descripcion,
            MultipartFile archivo) {
        if (materiaId == null || cursoId == null) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "materiaId y cursoId son obligatorios");
        }
        ProfesorMateria pm = profesorMateriaRepository
                .findByProfesorIdAndMateriaIdAndCursoId(profesor.getId(), materiaId, cursoId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No tiene asignada esa materia en ese curso. Solicite la asignación al administrador"));

        ArchivoGuardado guardado = almacenamiento.guardar(archivo, CARPETA);
        String nombre = titulo == null || titulo.isBlank() ? guardado.nombreOriginal() : titulo.trim();
        if (nombre.length() > 150) {
            throw new ValidacionException(ErrorCode.CAMPOS_INVALIDOS, "El título no puede superar 150 caracteres",
                    Map.of("titulo", "Máximo 150 caracteres"));
        }

        Recurso recurso = new Recurso();
        recurso.setNombre(nombre);
        recurso.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        recurso.setTipo(guardado.tipoContenido());
        recurso.setUrl(guardado.ruta());
        recurso.setNombreArchivo(guardado.nombreOriginal());
        recurso.setTamanoBytes(guardado.tamanoBytes());
        recurso.setProfesorMateria(pm);
        recurso.setFechaSubida(LocalDateTime.now());
        return MaterialDTO.from(recursoRepository.save(recurso));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargar(Usuario usuario, Long id) {
        Recurso recurso = obtener(id);
        exigirLectura(usuario, recurso);
        return almacenamiento.descargar(recurso.getUrl(), recurso.getNombreArchivo(), recurso.getTipo());
    }

    public void eliminar(Usuario usuario, Long id) {
        Recurso recurso = obtener(id);
        boolean esDueno = recurso.getProfesorMateria().getProfesor().getId().equals(usuario.getId());
        if (usuario.getRol() != Usuario.Rol.ADMIN && !esDueno) {
            throw new AccessDeniedException("Solo el profesor que publicó el material puede eliminarlo");
        }
        almacenamiento.eliminar(recurso.getUrl());
        recursoRepository.delete(recurso);
    }

    private Recurso obtener(Long id) {
        return recursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.MATERIAL_NO_ENCONTRADO, id));
    }

    private void exigirLectura(Usuario usuario, Recurso recurso) {
        ProfesorMateria pm = recurso.getProfesorMateria();
        boolean permitido = switch (usuario.getRol()) {
            case ADMIN -> true;
            case PROFESOR -> pm.getProfesor().getId().equals(usuario.getId())
                    || profesorMateriaRepository.existsByProfesorIdAndCursoId(usuario.getId(), pm.getCurso().getId());
            case ESTUDIANTE -> pm.getCurso().getId().equals(cursoDelEstudiante(usuario.getId()));
        };
        if (!permitido) {
            throw new AccessDeniedException("No tiene acceso a este material");
        }
    }

    private Long cursoDelEstudiante(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ErrorCode.ESTUDIANTE_NO_ENCONTRADO, estudianteId));
        if (estudiante.getCurso() == null) {
            throw new ValidacionException(ErrorCode.ESTUDIANTE_SIN_CURSO);
        }
        return estudiante.getCurso().getId();
    }
}
