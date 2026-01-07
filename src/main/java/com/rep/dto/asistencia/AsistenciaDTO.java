package com.rep.dto.asistencia;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AsistenciaDTO {
    private Long id; // null for new
    private Long estudianteId;
    private String estudianteNombre; // For fetching
    private String estado;
    private String tipoExcusa;
    private String observacion;
}
