package com.rep.service.impl;

import com.rep.model.Actividad;
import com.rep.model.RespuestaEstudiante;
import com.rep.model.RespuestaPregunta;
import com.rep.repositories.ActividadRepository;
import com.rep.repositories.RespuestaEstudianteRepository;
import com.rep.repositories.RespuestaPreguntaRepository;
import com.rep.service.archivos.ExcelReportGenerator;
import com.rep.service.archivos.PdfReportGenerator;
import com.rep.service.logica.RespuestaService;
import com.rep.exception.RecursoNoEncontradoException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class RespuestaServiceImpl implements RespuestaService {

    private final RespuestaEstudianteRepository respuestaEstudianteRepository;
    private final RespuestaPreguntaRepository respuestaPreguntaRepository;
private  final ActividadRepository actividadRepository;
    @Autowired
    public RespuestaServiceImpl(RespuestaEstudianteRepository respuestaEstudianteRepository,
                                RespuestaPreguntaRepository respuestaPreguntaRepository,ActividadRepository actividadRepository) {
        this.respuestaEstudianteRepository = respuestaEstudianteRepository;
        this.actividadRepository = actividadRepository;
        this.respuestaPreguntaRepository = respuestaPreguntaRepository;
    }

    @Override
    public List<RespuestaEstudiante> getRespuestasByActividad(Long actividadId) {
        return respuestaEstudianteRepository.findByActividadId(actividadId);
    }

    @Override
    public List<RespuestaEstudiante> getRespuestasByEstudiante(Long estudianteId) {
        return respuestaEstudianteRepository.findByEstudianteId(estudianteId);
    }

    @Override
    public RespuestaEstudiante calificarRespuesta(Long id, Float nota) {
        RespuestaEstudiante respuesta = respuestaEstudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        respuesta.setNota(nota);
        return respuestaEstudianteRepository.save(respuesta);
    }

    @Override
    public List<Object[]> getPromediosByCursoMateria(Long cursoId, Long materiaId) {
        return respuestaEstudianteRepository.findPromediosByCursoAndMateria(cursoId, materiaId);
    }

    @Override
    public List<Object[]> getEstadoEntregasByActividad(Long actividadId) {
        return respuestaEstudianteRepository.findEstadoEntregasByActividad(actividadId);
    }

    @Override
    public RespuestaPregunta guardarRespuestaPregunta(RespuestaPregunta respuestaPregunta) {
        return respuestaPreguntaRepository.save(respuestaPregunta);
    }

    // En RespuestaServiceImpl.java
    @Override
    public boolean profesorTieneAccesoARespuesta(Long profesorId, Long respuestaId) {
        return respuestaEstudianteRepository.existsByIdAndActividadProfesorMateriaProfesorId(respuestaId, profesorId);
    }

    @Override
    public boolean existeRespuesta(Long respuestaId) {
        return respuestaEstudianteRepository.existsById(respuestaId);
    }

    @Override
    public byte[] generatePromediosPdfReport(Long cursoId, Long materiaId) {
        List<Object[]> promedios = getPromediosByCursoMateria(cursoId, materiaId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("Reporte de Promedios"));
            document.add(new Paragraph(String.format("Curso: %d | Materia: %d", cursoId, materiaId)));

            PdfPTable table = new PdfPTable(2);
            table.addCell("Actividad ID");
            table.addCell("Promedio");

            promedios.forEach(arr -> {
                table.addCell(arr[0].toString());
                table.addCell(String.format("%.2f", arr[1]));
            });

            document.add(table);
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    @Override
    public List<RespuestaEstudiante> filtrarRespuestas(Long actividadId, Long cursoId, Long materiaId) {
        if (actividadId != null && cursoId == null && materiaId == null) {
            return getRespuestasByActividad(actividadId);
        } else if (actividadId == null && cursoId != null && materiaId != null) {
            return respuestaEstudianteRepository.findByCursoIdAndMateriaId(cursoId, materiaId);
        } else if (actividadId != null && cursoId != null && materiaId != null) {
            return respuestaEstudianteRepository.findByActividadIdAndCursoIdAndMateriaId(actividadId, cursoId, materiaId);
        } else {
            throw new IllegalArgumentException("Combinación de parámetros no soportada");
        }
    }


    public byte[] generarReporteResultados(Long cursoId, Long actividadId, String formato) {
        try {
            List<RespuestaEstudiante> respuestas = respuestaEstudianteRepository
                    .findByActividadIdAndCursoId(actividadId, cursoId);

            Actividad actividad = actividadRepository.findById(actividadId)
                    .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

            switch (formato.toLowerCase()) {
                case "pdf":
                    return new PdfReportGenerator().generate(respuestas, actividad);
                case "xlsx":
                    return new ExcelReportGenerator().generate(respuestas, actividad);
                default:
                    throw new IllegalArgumentException("Formato no soportado: " + formato);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte", e);
        }
    }
}