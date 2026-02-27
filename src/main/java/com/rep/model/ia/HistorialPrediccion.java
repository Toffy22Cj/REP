package com.rep.model.ia;

import com.rep.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "historial_predicciones", catalog = "rep_ia")
public class HistorialPrediccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Column(name = "tipo_prediccion", nullable = false, length = 50)
    private String tipoPrediccion;

    @Column(name = "probabilidad_riesgo", precision = 5, scale = 4)
    private BigDecimal probabilidadRiesgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo")
    private NivelRiesgo nivelRiesgo;

    @Column(name = "features_usados", columnDefinition = "JSON")
    private String featuresUsados;

    @Column(name = "factores_principales", columnDefinition = "JSON")
    private String factoresPrincipales;

    @Column(name = "recomendaciones", columnDefinition = "JSON")
    private String recomendaciones;

    @Column(name = "modelo_version", length = 50)
    private String modeloVersion;

    @Column(precision = 5, scale = 4)
    private BigDecimal confianza;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;

    public enum NivelRiesgo {
        BAJO, MEDIO, ALTO
    }
}
