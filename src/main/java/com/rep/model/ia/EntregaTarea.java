package com.rep.model.ia;

import com.rep.model.Materia;
import com.rep.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "entregas_tareas", catalog = "rep_ia")
public class EntregaTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    // TODO: Link to Actividad entity when available in main model
    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "minutos_retraso", insertable = false, updatable = false)
    private Integer minutosRetraso;

    // Estado is calculated in SQL View or App logic, not stored directly if
    // generated column was problematic
    // But we can add a transient getter
    @Transient
    public String getEstado() {
        if (fechaEntrega == null) {
            return LocalDateTime.now().isBefore(fechaLimite) ? "PENDIENTE" : "NO_ENTREGADA";
        }
        return fechaEntrega.isBefore(fechaLimite) ? "ENTREGADA_A_TIEMPO" : "ENTREGADA_TARDE";
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
