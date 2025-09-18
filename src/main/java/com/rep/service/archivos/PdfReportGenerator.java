package com.rep.service.archivos;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.rep.model.Actividad;
import com.rep.model.RespuestaEstudiante;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class PdfReportGenerator {
    public byte[] generate(List<RespuestaEstudiante> respuestas, Actividad actividad) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdf);

        // Título del reporte
        document.add(new Paragraph("Resultados de la Actividad: " + actividad.getTitulo())
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        // Tabla de resultados
        Table table = new Table(2); // 2 columnas: Estudiante y Calificación
        table.addHeaderCell("Estudiante");
        table.addHeaderCell("Calificación");

        // Llenar la tabla con datos
        respuestas.forEach(respuesta -> {
            table.addCell(respuesta.getEstudiante().getNombre());
            table.addCell(String.format("%.2f", respuesta.getNota()));
        });

        document.add(table);
        document.close();
        return baos.toByteArray();
    }
}
