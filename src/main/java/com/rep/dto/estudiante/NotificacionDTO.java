package com.rep.dto.estudiante;



import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificacionDTO {
    private Long id;
    private String mensaje;
    private boolean leida;
    private LocalDateTime fechaCreacion;
    private Long actividadId; // Opcional
    private String tituloActividad; // Opcional
}