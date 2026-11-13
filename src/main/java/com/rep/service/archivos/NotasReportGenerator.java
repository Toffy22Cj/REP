package com.rep.service.archivos;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.rep.dto.notas.NotasDTOs.*;
import com.rep.exception.ErrorCode;
import com.rep.exception.RepException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Exportación del boletín (PDF) y de la planilla de notas (PDF y Excel). */
@Component
public class NotasReportGenerator {
    private static final DeviceRgb PRIMARIO = new DeviceRgb(79, 70, 229);
    private static final DeviceRgb GRIS = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb ROJO = new DeviceRgb(185, 28, 28);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES = Locale.forLanguageTag("es-CO");

    // ================================================================== boletín PDF

    public byte[] boletinPdf(Boletin b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(new PdfDocument(new PdfWriter(out)), PageSize.LETTER)) {
            doc.add(titulo("Boletín de calificaciones"));
            doc.add(new Paragraph(b.estudiante() + "  ·  Identificación " + b.identificacion() + "  ·  Curso " + b.curso())
                    .setFontSize(10));
            doc.add(new Paragraph("Generado el " + b.generadoEn().format(FECHA) + "  ·  Nota mínima aprobatoria "
                    + nota(b.notaMinimaAprobatoria())).setFontSize(9).setFontColor(ColorConstants.GRAY));

            Table resumen = new Table(UnitValue.createPercentArray(new float[] { 34, 26, 12, 14, 14 })).useAllAvailableWidth();
            for (String h : new String[] { "Materia", "Profesor", "Promedio", "Desempeño", "Fallas" }) {
                resumen.addHeaderCell(encabezado(h));
            }
            for (NotasMateria m : b.materias()) {
                resumen.addCell(celda(m.materia()));
                resumen.addCell(celda(m.profesor()));
                resumen.addCell(celdaNota(m.promedio(), b.notaMinimaAprobatoria()));
                resumen.addCell(celda(textoDesempeno(m.desempeno())));
                resumen.addCell(celda(String.valueOf(m.asistencia().ausencias())).setTextAlignment(TextAlignment.CENTER));
            }
            resumen.addCell(new Cell(1, 2).add(new Paragraph("Promedio general").setBold()).setFontSize(9));
            resumen.addCell(celdaNota(b.promedioGeneral(), b.notaMinimaAprobatoria()).setBold());
            resumen.addCell(new Cell(1, 2).add(new Paragraph(textoDesempeno(b.desempenoGeneral()))).setFontSize(9));
            doc.add(resumen);

            for (NotasMateria m : b.materias()) {
                if (m.actividades().isEmpty()) continue;
                doc.add(new Paragraph(m.materia()).setBold().setFontSize(11).setMarginTop(12).setFontColor(PRIMARIO));
                Table t = new Table(UnitValue.createPercentArray(new float[] { 44, 14, 16, 12, 14 })).useAllAvailableWidth();
                for (String h : new String[] { "Actividad", "Tipo", "Entrega", "Nota", "Estado" }) {
                    t.addHeaderCell(encabezado(h));
                }
                for (NotaActividad a : m.actividades()) {
                    t.addCell(celda(a.titulo()));
                    t.addCell(celda(a.tipo()));
                    t.addCell(celda(a.fechaEntrega() != null ? a.fechaEntrega().toString() : "-"));
                    t.addCell(celdaNota(a.nota(), b.notaMinimaAprobatoria()));
                    t.addCell(celda(textoEstado(a.estado())));
                }
                doc.add(t);
            }
        }
        return out.toByteArray();
    }

    // ================================================================== planilla PDF

    public byte[] planillaPdf(Planilla p) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(new PdfDocument(new PdfWriter(out)), PageSize.LETTER.rotate())) {
            doc.setMargins(24, 24, 24, 24);
            doc.add(titulo("Planilla de notas · " + p.materia() + " · " + p.curso()));
            doc.add(new Paragraph("Generado el " + p.generadoEn().format(FECHA) + "  ·  Promedio del curso "
                    + nota(p.promedioCurso()) + "  ·  Aprobados " + p.aprobados() + "  ·  Reprobados " + p.reprobados())
                    .setFontSize(9).setFontColor(ColorConstants.GRAY));

            int n = p.actividades().size();
            float[] anchos = new float[n + 4];
            anchos[0] = 22;
            for (int i = 1; i <= n; i++) anchos[i] = 60f / Math.max(1, n);
            anchos[n + 1] = 6; anchos[n + 2] = 7; anchos[n + 3] = 5;
            Table t = new Table(UnitValue.createPercentArray(anchos)).useAllAvailableWidth().setFontSize(8);
            t.addHeaderCell(encabezado("Estudiante"));
            p.actividades().forEach(a -> t.addHeaderCell(encabezado(a.titulo())));
            t.addHeaderCell(encabezado("Prom."));
            t.addHeaderCell(encabezado("Desempeño"));
            t.addHeaderCell(encabezado("Fallas"));
            for (FilaEstudiante f : p.estudiantes()) {
                t.addCell(celda(f.estudiante()));
                f.notas().forEach(c -> t.addCell(c.nota() != null ? celdaNota(c.nota(), p.notaMinimaAprobatoria())
                        : celda(c.estado() == EstadoNota.VENCIDA ? "No entregó" : "—").setTextAlignment(TextAlignment.CENTER)));
                t.addCell(celdaNota(f.promedio(), p.notaMinimaAprobatoria()).setBold());
                t.addCell(celda(textoDesempeno(f.desempeno())));
                t.addCell(celda(String.valueOf(f.ausencias())).setTextAlignment(TextAlignment.CENTER));
            }
            t.addCell(celda("Promedio por actividad").setBold());
            p.actividades().forEach(a -> t.addCell(celdaNota(a.promedio(), p.notaMinimaAprobatoria())));
            t.addCell(celdaNota(p.promedioCurso(), p.notaMinimaAprobatoria()).setBold());
            t.addCell(celda(""));
            t.addCell(celda(""));
            doc.add(t);
        }
        return out.toByteArray();
    }

    // ================================================================== planilla Excel

    public byte[] planillaExcel(Planilla p) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Notas");
            CellStyle titulo = wb.createCellStyle();
            Font negrita = wb.createFont();
            negrita.setBold(true);
            titulo.setFont(negrita);
            CellStyle cabecera = wb.createCellStyle();
            cabecera.setFont(negrita);
            cabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecera.setWrapText(true);
            CellStyle numero = wb.createCellStyle();
            numero.setDataFormat(wb.createDataFormat().getFormat("0.0"));
            CellStyle reprobado = wb.createCellStyle();
            reprobado.cloneStyleFrom(numero);
            Font roja = wb.createFont();
            roja.setColor(IndexedColors.RED.getIndex());
            reprobado.setFont(roja);

            org.apache.poi.ss.usermodel.Row r0 = hoja.createRow(0);
            r0.createCell(0).setCellValue("Planilla de notas · " + p.materia() + " · " + p.curso());
            r0.getCell(0).setCellStyle(titulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(3, p.actividades().size() + 4)));
            hoja.createRow(1).createCell(0).setCellValue("Generado el " + p.generadoEn().format(FECHA)
                    + " · Nota mínima " + p.notaMinimaAprobatoria());

            org.apache.poi.ss.usermodel.Row h = hoja.createRow(3);
            int c = 0;
            for (String t : new String[] { "Estudiante", "Identificación" }) {
                celdaTexto(h, c++, t, cabecera);
            }
            for (ColumnaActividad a : p.actividades()) {
                celdaTexto(h, c++, a.titulo() + " (" + a.tipo() + ")", cabecera);
            }
            for (String t : new String[] { "Promedio", "Desempeño", "Fallas" }) {
                celdaTexto(h, c++, t, cabecera);
            }

            int fila = 4;
            for (FilaEstudiante f : p.estudiantes()) {
                org.apache.poi.ss.usermodel.Row r = hoja.createRow(fila++);
                int col = 0;
                r.createCell(col++).setCellValue(f.estudiante());
                r.createCell(col++).setCellValue(f.identificacion());
                for (CeldaNota n : f.notas()) {
                    org.apache.poi.ss.usermodel.Cell cell = r.createCell(col++);
                    if (n.nota() != null) {
                        cell.setCellValue(n.nota());
                        cell.setCellStyle(n.nota() < p.notaMinimaAprobatoria() ? reprobado : numero);
                    } else {
                        cell.setCellValue(n.estado() == EstadoNota.VENCIDA ? "No entregó" : "");
                    }
                }
                org.apache.poi.ss.usermodel.Cell prom = r.createCell(col++);
                if (f.promedio() != null) {
                    prom.setCellValue(f.promedio());
                    prom.setCellStyle(Boolean.FALSE.equals(f.aprobada()) ? reprobado : numero);
                }
                r.createCell(col++).setCellValue(textoDesempeno(f.desempeno()));
                r.createCell(col).setCellValue(f.ausencias());
            }
            hoja.setColumnWidth(0, 30 * 256);
            hoja.setColumnWidth(1, 15 * 256);
            for (int i = 2; i < p.actividades().size() + 5; i++) {
                hoja.setColumnWidth(i, 14 * 256);
            }
            hoja.createFreezePane(2, 4);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RepException(ErrorCode.REPORTE_NO_GENERADO, null, null, e);
        }
    }

    // ================================================================== helpers

    private static Paragraph titulo(String texto) {
        return new Paragraph(texto).setFontSize(16).setBold().setFontColor(PRIMARIO);
    }

    private static Cell encabezado(String texto) {
        return new Cell().add(new Paragraph(texto).setBold()).setBackgroundColor(GRIS).setFontSize(9);
    }

    private static Cell celda(String texto) {
        return new Cell().add(new Paragraph(texto == null ? "" : texto)).setFontSize(9);
    }

    private static Cell celdaNota(Float nota, float minima) {
        Cell c = celda(nota(nota)).setTextAlignment(TextAlignment.CENTER);
        if (nota != null && nota < minima) {
            c.setFontColor(ROJO);
        }
        return c;
    }

    private static void celdaTexto(org.apache.poi.ss.usermodel.Row r, int col, String texto, CellStyle estilo) {
        org.apache.poi.ss.usermodel.Cell cell = r.createCell(col);
        cell.setCellValue(texto);
        cell.setCellStyle(estilo);
    }

    static String nota(Float n) {
        return n == null ? "—" : String.format(ES, "%.1f", n);
    }

    static String textoDesempeno(String d) {
        if (d == null) return "—";
        return switch (d) {
            case "SUPERIOR" -> "Superior";
            case "ALTO" -> "Alto";
            case "BASICO" -> "Básico";
            default -> "Bajo";
        };
    }

    static String textoEstado(EstadoNota e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente";
            case EN_CURSO -> "En curso";
            case ENTREGADA -> "Por revisar";
            case CALIFICADA -> "Calificada";
            case VENCIDA -> "No entregó";
        };
    }
}
