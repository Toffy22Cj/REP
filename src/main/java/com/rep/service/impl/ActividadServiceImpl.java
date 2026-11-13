package com.rep.service.impl;

import com.rep.dto.actividad.ActividadConPreguntasDTO;
import com.rep.dto.actividad.OpcionDTO;
import com.rep.dto.actividad.PreguntaConOpcionesDTO;
import com.rep.model.Actividad;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.ProfesorMateria;
import com.rep.repositories.ActividadRepository;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.RespuestaEstudianteRepository;
import com.rep.service.logica.ActividadService;
import com.rep.exception.RecursoNoEncontradoException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@org.springframework.transaction.annotation.Transactional
public class ActividadServiceImpl implements ActividadService {

    private final ActividadRepository actividadRepository;
    private final RespuestaEstudianteRepository respuestaEstudianteRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final com.rep.repositories.EstudianteRepository estudianteRepository;

    public ActividadServiceImpl(ActividadRepository actividadRepository,
            RespuestaEstudianteRepository respuestaEstudianteRepository,
            ProfesorMateriaRepository profesorMateriaRepository,
            com.rep.repositories.EstudianteRepository estudianteRepository) {
        this.actividadRepository = actividadRepository;
        this.respuestaEstudianteRepository = respuestaEstudianteRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.estudianteRepository = estudianteRepository;
    }

    @Override
    public Actividad crearActividad(Actividad actividad) {
        try {
            // Validación básica
            if (actividad.getTitulo() == null || actividad.getTitulo().trim().isEmpty()) {
                throw new IllegalArgumentException("El título de la actividad no puede estar vacío");
            }

            // Validar relación Profesor-Materia
            ProfesorMateria pm = actividad.getProfesorMateria();
            if (pm == null || pm.getId() == null) {
                throw new IllegalArgumentException(
                        "La actividad debe estar asociada a una relación Profesor-Materia válida");
            }

            // Verificar existencia de la relación
            ProfesorMateria relacionExistente = profesorMateriaRepository.findById(pm.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe la relación Profesor-Materia con ID: " + pm.getId()));

            // Validar coherencia entre la relación y el profesor asignado
            if (!relacionExistente.getProfesor().getId().equals(actividad.getProfesor().getId())) {
                throw new IllegalArgumentException(
                        "El profesor de la actividad no coincide con la relación Profesor-Materia");
            }

            // Establecer fechas
            actividad.setFechaCreacion(LocalDateTime.now());

            // Guardar actividad
            Actividad actividadGuardada = actividadRepository.save(actividad);
            log.info("Actividad creada con ID: {}", actividadGuardada.getId());

            return actividadGuardada;

        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad de datos al crear actividad", e);
            throw new IllegalArgumentException("Error de datos: " + e.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public Actividad editarActividad(Long id, Actividad actividad) {
        Actividad existente = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));
        existente.setTitulo(actividad.getTitulo());
        existente.setTipo(actividad.getTipo());
        existente.setDescripcion(actividad.getDescripcion());
        existente.setFechaEntrega(actividad.getFechaEntrega());
        // La duración y el estado no se copiaban: editarlos no tenía efecto.
        if (actividad.getDuracionMinutos() != null) {
            existente.setDuracionMinutos(actividad.getDuracionMinutos());
        }
        if (actividad.getActiva() != null) {
            existente.setActiva(actividad.getActiva());
        }
        return actividadRepository.save(existente);
    }

    @Override
    public void eliminarActividad(Long id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        if (respuestaEstudianteRepository.existsByActividadId(id)) {
            throw new IllegalStateException("No se puede eliminar: La actividad tiene respuestas asociadas");
        }
        actividadRepository.deleteById(id);
    }

    @Override
    public List<Actividad> listarActividades(Long materiaId, Long cursoId) {
        if (materiaId != null && cursoId != null) {
            return actividadRepository.findByProfesorMateriaMateriaIdAndProfesorMateriaCursoId(materiaId, cursoId);
        } else if (materiaId != null) {
            return actividadRepository.findByProfesorMateriaMateriaId(materiaId);
        } else if (cursoId != null) {
            return actividadRepository.findByProfesorMateriaCursoId(cursoId);
        }
        return actividadRepository.findAll();
    }

    @Override
    public List<Actividad> listarActividadesPorProfesor(Long materiaId, Long cursoId, Long profesorId) {
        if (materiaId != null && cursoId != null) {
            return actividadRepository.findByProfesorMateriaMateriaIdAndProfesorMateriaCursoIdAndProfesorId(
                    materiaId, cursoId, profesorId);
        } else if (materiaId != null) {
            return actividadRepository.findByProfesorMateriaMateriaIdAndProfesorId(materiaId, profesorId);
        } else if (cursoId != null) {
            return actividadRepository.findByProfesorMateriaCursoIdAndProfesorId(cursoId, profesorId);
        }
        return actividadRepository.findByProfesorId(profesorId);
    }

    @Override
    public Optional<Actividad> getActividadById(Long id) {
        return actividadRepository.findById(id);
    }

    @Override
    public List<Object[]> obtenerDistribucionRespuestas(Long preguntaId) {
        // Implementación similar a PreguntaService o ajustada para Actividad
        return respuestaEstudianteRepository.findDistribucionRespuestasByPregunta(preguntaId);
    }

    @Override
    public boolean profesorTieneAccesoAActividad(Long profesorId, Long actividadId) {
        log.debug("Validando acceso para profesor {} a actividad {}", profesorId, actividadId);

        // Opción 1: Más eficiente (una sola consulta)
        boolean tieneAcceso = actividadRepository.existsByIdAndProfesorMateria_Profesor_Id(actividadId, profesorId);

        // Opción 2: Más explícita (dos consultas)
        // boolean tieneAcceso =
        // actividadRepository.findProfesorIdByActividadId(actividadId)
        // .map(id -> id.equals(profesorId))
        // .orElse(false);

        log.debug("Resultado validación: {}", tieneAcceso);
        return tieneAcceso;
    }

    @Override
    public boolean existeActividad(Long actividadId) {
        return actividadRepository.existsById(actividadId);
    }

    @Override
    public Map<String, Object> getEstadisticasActividad(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        int totalEstudiantes = actividad.getProfesorMateria().getCurso().getEstudiantes().size();
        List<com.rep.model.RespuestaEstudiante> respuestas = respuestaEstudianteRepository.findByActividadId(actividadId);
        // Antes se sumaban también los intentos sin entregar y, con un curso vacío, el
        // porcentaje era una división por cero (NaN/Infinity en el JSON).
        long entregadas = respuestas.stream().filter(r -> Boolean.TRUE.equals(r.getEntregado())).count();
        long enCurso = respuestas.size() - entregadas;
        java.util.DoubleSummaryStatistics notas = respuestas.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEntregado()) && r.getNota() != null)
                .mapToDouble(r -> r.getNota()).summaryStatistics();
        long revisadas = respuestas.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEntregado()) && r.getCalificadoPor() != null).count();

        Map<String, Object> resultado = new java.util.LinkedHashMap<>();
        resultado.put("totalEstudiantes", totalEstudiantes);
        resultado.put("entregasRealizadas", entregadas);
        resultado.put("porcentajeEntregas", totalEstudiantes == 0 ? 0.0
                : Math.round(entregadas * 1000.0 / totalEstudiantes) / 10.0);
        resultado.put("enCurso", enCurso);
        resultado.put("sinIniciar", Math.max(0, totalEstudiantes - respuestas.size()));
        resultado.put("revisadas", revisadas);
        resultado.put("promedio", notas.getCount() == 0 ? null : Math.round(notas.getAverage() * 10) / 10.0);
        resultado.put("notaMaxima", notas.getCount() == 0 ? null : Math.round(notas.getMax() * 10) / 10.0);
        resultado.put("notaMinima", notas.getCount() == 0 ? null : Math.round(notas.getMin() * 10) / 10.0);
        resultado.put("preguntas", actividad.getPreguntas() == null ? 0 : actividad.getPreguntas().size());
        return resultado;
    }

    @Override
    public Map<String, Object> getDashboardData(Long profesorId) {
        long totalActividades = actividadRepository.countByProfesorMateriaProfesorId(profesorId);
        long actividadesRecientes = actividadRepository.countByProfesorMateriaProfesorIdAndFechaCreacionAfter(
                profesorId,
                LocalDateTime.now().minusDays(7));

        // Las tres últimas claves son las que muestra el cliente de escritorio; antes no
        // se enviaban y el dashboard del profesor mostraba siempre 0.
        return Map.of(
                "totalActividades", totalActividades,
                "actividadesRecientes", actividadesRecientes,
                "cursosAsignados", profesorMateriaRepository.countDistinctCursoByProfesorId(profesorId),
                "totalEstudiantes", estudianteRepository.countEstudiantesDeProfesor(profesorId),
                "actividadesActivas", actividadRepository.countByProfesorMateriaProfesorIdAndActivaTrue(profesorId),
                "entregasPendientes", respuestaEstudianteRepository.countEntregasPendientesDeRevision(
                        profesorId, com.rep.model.Pregunta.TipoPregunta.RESPUESTA_ABIERTA));
    }

    @Override
    public List<Actividad> buscarPorTituloYProfesor(String titulo, Long profesorId) {
        return actividadRepository.findByTituloAndProfesorId(titulo, profesorId);
    }

    @Override
    public ActividadConPreguntasDTO getActividadConPreguntas(Long actividadId) {
        Actividad actividad = actividadRepository.findByIdWithPreguntasAndOpciones(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        // Verifica que las preguntas y opciones se cargaron
        if (actividad.getPreguntas() != null) {
            log.debug("Actividad tiene {} preguntas", actividad.getPreguntas().size());
            actividad.getPreguntas().forEach(p -> {
                if (p.getOpciones() != null) {
                    log.debug("Pregunta {} tiene {} opciones", p.getId(), p.getOpciones().size());
                }
            });
        }

        return mapToDto(actividad);
    }

    private ActividadConPreguntasDTO mapToDto(Actividad actividad) {
        ActividadConPreguntasDTO dto = new ActividadConPreguntasDTO();
        dto.setId(actividad.getId());
        dto.setTitulo(actividad.getTitulo());
        dto.setDescripcion(actividad.getDescripcion());
        // Orden estable: las colecciones son Set y antes el orden variaba entre peticiones
        dto.setPreguntas(actividad.getPreguntas().stream()
                .sorted(java.util.Comparator.comparing(Pregunta::getId))
                .map(this::mapPreguntaToDto)
                .toList());
        return dto;
    }

    private PreguntaConOpcionesDTO mapPreguntaToDto(Pregunta pregunta) {
        PreguntaConOpcionesDTO preguntaDto = new PreguntaConOpcionesDTO();
        preguntaDto.setId(pregunta.getId());
        preguntaDto.setEnunciado(pregunta.getEnunciado());
        preguntaDto.setTipo(pregunta.getTipo());
        preguntaDto.setOpciones(pregunta.getOpciones().stream()
                .sorted(java.util.Comparator.comparing(Opcion::getId))
                .map(this::mapOpcionToDto)
                .toList());

        // Se expone la URL de descarga de la API, nunca la ruta interna del servidor
        if (pregunta.getArchivoUrl() != null && !pregunta.getArchivoUrl().isBlank()) {
            preguntaDto.setArchivoDisponible(true);
            preguntaDto.setNombreArchivo(pregunta.getNombreArchivo() != null
                    ? pregunta.getNombreArchivo() : new java.io.File(pregunta.getArchivoUrl()).getName());
            preguntaDto.setArchivoUrl("/api/preguntas/" + pregunta.getId() + "/archivo");
            preguntaDto.setArchivoTipo(pregunta.getArchivoTipo());
            preguntaDto.setArchivoEsImagen(pregunta.getArchivoTipo() != null && pregunta.getArchivoTipo().startsWith("image/"));
        }
        return preguntaDto;
    }

    private OpcionDTO mapOpcionToDto(Opcion opcion) {
        OpcionDTO opcionDto = new OpcionDTO();
        opcionDto.setId(opcion.getId());
        opcionDto.setTexto(opcion.getTexto());
        opcionDto.setEsCorrecta(opcion.getEsCorrecta());
        if (opcion.getArchivoUrl() != null && !opcion.getArchivoUrl().isBlank()) {
            opcionDto.setArchivoDisponible(true);
            opcionDto.setNombreArchivo(opcion.getNombreArchivo() != null
                    ? opcion.getNombreArchivo() : new java.io.File(opcion.getArchivoUrl()).getName());
            opcionDto.setArchivoUrl("/api/preguntas/opciones/" + opcion.getId() + "/archivo");
        }
        return opcionDto;
    }

    @Override
    public List<com.rep.dto.actividad.ResultadoActividadDTO> obtenerResultados(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada"));

        List<com.rep.model.RespuestaEstudiante> respuestas = respuestaEstudianteRepository
                .findByActividadId(actividadId);

        // Aquí deberíamos mapear también a aquellos estudiantes que NO han respondido
        // aún,
        // pero por ahora devolvemos las respuestas existentes.

        return respuestas.stream().map(respuesta -> {
            com.rep.dto.actividad.ResultadoActividadDTO dto = new com.rep.dto.actividad.ResultadoActividadDTO();
            dto.setActividadId(actividadId);
            dto.setEstudianteId(respuesta.getEstudiante().getId());
            dto.setNota(respuesta.getNota());
            dto.setObservaciones(respuesta.getObservaciones());
            dto.setNombreEstudiante(respuesta.getEstudiante().getNombre());
            if (respuesta.getFechaEntrega() != null) {
                dto.setFechaEntrega(respuesta.getFechaEntrega()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else {
                dto.setFechaEntrega("Pendiente");
            }
            // No cargamos los detalles de preguntas aquí para no sobrecargar, se cargan en
            // obtenerRespuestaEstudiante
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public com.rep.model.RespuestaEstudiante obtenerRespuestaEstudiante(Long actividadId, Long estudianteId) {
        com.rep.model.RespuestaEstudiante respuesta = respuestaEstudianteRepository
                .findByEstudianteIdAndActividadId(estudianteId, actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada para el estudiante"));

        // Forzar inicialización de lazy loading
        respuesta.getRespuestasPreguntas().size();
        respuesta.getRespuestasPreguntas().forEach(rp -> {
            // Inicializar preguntas y opciones también si es necesario
            rp.getPregunta().getEnunciado();
            if (rp.getOpcion() != null)
                rp.getOpcion().getTexto();
            // Inicializar campos de archivo adjunto (pueden ser null)
            if (rp.getArchivoAdjunto() != null)
                rp.getArchivoAdjunto();
            if (rp.getNombreArchivo() != null)
                rp.getNombreArchivo();
        });

        return respuesta;
    }

    @Override
    public com.rep.model.RespuestaEstudiante actualizarNota(Long actividadId, Long estudianteId, Float nuevaNota,
            String observaciones, com.rep.model.Profesor profesor) {
        com.rep.service.logica.EscalaCalificacion.validar(nuevaNota);
        com.rep.model.RespuestaEstudiante respuesta = respuestaEstudianteRepository
                .findByEstudianteIdAndActividadId(estudianteId, actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        respuesta.setNota(nuevaNota);
        if (observaciones != null) {
            respuesta.setObservaciones(observaciones);
        }
        if (profesor != null) {
            respuesta.setCalificadoPor(profesor);
        }

        return respuestaEstudianteRepository.save(respuesta);
    }

    @Override
    public Actividad clonarActividad(Long id) {
        Actividad original = actividadRepository.findByIdWithPreguntasAndOpciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada con ID: " + id));

        Actividad clon = new Actividad();
        clon.setTitulo(original.getTitulo() + " (Copia)");
        clon.setTipo(original.getTipo());
        clon.setDescripcion(original.getDescripcion());
        clon.setFechaEntrega(original.getFechaEntrega());
        clon.setHoraEntrega(original.getHoraEntrega());
        clon.setDuracionMinutos(original.getDuracionMinutos());
        clon.setProfesorMateria(original.getProfesorMateria());
        // Sin el profesor, la copia no aparecía en su lista ni se podía editar.
        clon.setProfesor(original.getProfesor() != null ? original.getProfesor()
                : original.getProfesorMateria().getProfesor());
        clon.setPermitirReintentos(original.getPermitirReintentos());
        clon.setActiva(true);
        clon.setFechaCreacion(LocalDateTime.now());

        if (original.getPreguntas() != null) {
            for (Pregunta pOriginal : original.getPreguntas()) {
                Pregunta pClon = new Pregunta();
                pClon.setEnunciado(pOriginal.getEnunciado());
                pClon.setTipo(pOriginal.getTipo());
                pClon.setArchivoUrl(pOriginal.getArchivoUrl());
                pClon.setArchivoTipo(pOriginal.getArchivoTipo());
                pClon.setActividad(clon);

                if (pOriginal.getOpciones() != null) {
                    for (Opcion oOriginal : pOriginal.getOpciones()) {
                        Opcion oClon = new Opcion();
                        oClon.setTexto(oOriginal.getTexto());
                        oClon.setEsCorrecta(oOriginal.getEsCorrecta());
                        oClon.setArchivoUrl(oOriginal.getArchivoUrl());
                        oClon.setArchivoTipo(oOriginal.getArchivoTipo());
                        oClon.setNombreArchivo(oOriginal.getNombreArchivo());
                        oClon.setTieneArchivo(oOriginal.getTieneArchivo());
                        pClon.addOpcion(oClon);
                    }
                }
                clon.getPreguntas().add(pClon);
            }
        }

        Actividad guardada = actividadRepository.save(clon);
        log.info("Actividad ID {} clonada exitosamente con nuevo ID {}", id, guardada.getId());
        return guardada;
    }
}
