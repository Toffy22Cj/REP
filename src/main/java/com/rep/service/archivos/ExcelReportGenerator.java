package com.rep.service.archivos;

import com.rep.model.Actividad;
import com.rep.model.RespuestaEstudiante;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelReportGenerator {
    public byte[] generate(List<RespuestaEstudiante> respuestas, Actividad actividad) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Resultados");

        // Estilo para encabezados
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Crear fila de encabezados
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Estudiante");
        headerRow.createCell(1).setCellValue("Calificación");
        headerRow.forEach(cell -> cell.setCellStyle(headerStyle));

        // Llenar datos
        int rowNum = 1;
        for (RespuestaEstudiante respuesta : respuestas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(respuesta.getEstudiante().getNombre());
            row.createCell(1).setCellValue(respuesta.getNota());
        }

        // Autoajustar columnas
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        // Convertir a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }
}