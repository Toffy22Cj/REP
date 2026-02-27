-- ============================================
-- REP 2.0: Base de Datos de Inteligencia Artificial
-- Versión: 1.0.0
-- Descripción: Tablas de sensores de comportamiento para predicciones de IA
-- ============================================

-- Crear base de datos separada para datos de IA
CREATE DATABASE IF NOT EXISTS rep_ia 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE rep_ia;

-- ============================================
-- Tabla: asistencias
-- Descripción: Registro de asistencia diaria por estudiante y materia
-- ============================================
CREATE TABLE asistencias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL COMMENT 'FK a usuarios.id del sistema principal',
    materia_id BIGINT NOT NULL COMMENT 'FK a materias.id del sistema principal',
    fecha DATE NOT NULL,
    presente BOOLEAN NOT NULL DEFAULT FALSE,
    observaciones VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_estudiante_fecha (estudiante_id, fecha),
    INDEX idx_materia_fecha (materia_id, fecha),
    INDEX idx_fecha (fecha),
    UNIQUE KEY idx_unico_asistencia (estudiante_id, materia_id, fecha)
) ENGINE=InnoDB COMMENT='Registro diario de asistencia de estudiantes';

-- ============================================
-- Tabla: entregas_tareas
-- Descripción: Registro de entregas de tareas y retrasos
-- ============================================
CREATE TABLE entregas_tareas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL COMMENT 'FK a usuarios.id del sistema principal',
    materia_id BIGINT NOT NULL COMMENT 'FK a materias.id del sistema principal',
    actividad_id BIGINT COMMENT 'FK a actividades.id del sistema principal (nullable)',
    titulo VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    fecha_limite DATETIME NOT NULL,
    fecha_entrega DATETIME,
    minutos_retraso INT GENERATED ALWAYS AS (
        CASE 
            WHEN fecha_entrega IS NULL THEN NULL
            WHEN fecha_entrega > fecha_limite THEN TIMESTAMPDIFF(MINUTE, fecha_limite, fecha_entrega)
            ELSE 0
        END
    ) STORED COMMENT 'Calculado automáticamente: minutos de retraso',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_estudiante_materia (estudiante_id, materia_id),
    INDEX idx_fecha_limite (fecha_limite)
) ENGINE=InnoDB COMMENT='Registro de entregas de tareas y sus retrasos';


-- ============================================
-- Tabla: intentos_evaluacion
-- Descripción: Registro de intentos de respuesta para evaluación semántica
-- ============================================
CREATE TABLE intentos_evaluacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL COMMENT 'FK a usuarios.id del sistema principal',
    pregunta_id BIGINT NOT NULL COMMENT 'FK a preguntas.id del sistema principal',
    actividad_id BIGINT COMMENT 'FK a actividades.id del sistema principal (nullable)',
    respuesta_texto TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    respuesta_correcta TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    similitud_semantica DECIMAL(5,4) COMMENT 'Valor entre 0.0000 y 1.0000',
    es_correcta BOOLEAN,
    metodo_evaluacion ENUM('EXACTA', 'SEMANTICA', 'PARCIAL', 'MANUAL') DEFAULT 'SEMANTICA',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_estudiante_pregunta (estudiante_id, pregunta_id),
    INDEX idx_actividad (actividad_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_es_correcta (es_correcta)
) ENGINE=InnoDB COMMENT='Intentos de respuesta con evaluación semántica';

-- ============================================
-- Tabla: logs_actividad
-- Descripción: Logs de actividad del estudiante en el sistema
-- ============================================
CREATE TABLE logs_actividad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL COMMENT 'FK a usuarios.id del sistema principal',
    accion VARCHAR(100) NOT NULL COMMENT 'LOGIN, LOGOUT, VER_MATERIA, RESPONDER_EVALUACION, etc.',
    materia_id BIGINT COMMENT 'FK a materias.id si aplica',
    actividad_id BIGINT COMMENT 'FK a actividades.id si aplica',
    duracion_segundos INT COMMENT 'Duración de la sesión/actividad en segundos',
    metadata JSON COMMENT 'Datos adicionales en formato JSON',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_estudiante_timestamp (estudiante_id, timestamp),
    INDEX idx_accion (accion),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB COMMENT='Logs de actividad de estudiantes en el sistema';

-- ============================================
-- Tabla: historial_predicciones
-- Descripción: Historial de predicciones de IA sobre estudiantes
-- ============================================
CREATE TABLE historial_predicciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL COMMENT 'FK a usuarios.id del sistema principal',
    tipo_prediccion VARCHAR(50) NOT NULL COMMENT 'RIESGO_ACADEMICO, DESERCION, RENDIMIENTO, etc.',
    probabilidad_riesgo DECIMAL(5,4) COMMENT 'Probabilidad entre 0.0000 y 1.0000',
    nivel_riesgo ENUM('BAJO', 'MEDIO', 'ALTO'),
    features_usados JSON COMMENT 'Features utilizados para la predicción',
    factores_principales JSON COMMENT 'Factores que más influyeron en la predicción',
    recomendaciones JSON COMMENT 'Recomendaciones generadas por la IA',
    modelo_version VARCHAR(50) COMMENT 'Versión del modelo usado (ej: rf_v1.0, nn_v2.1)',
    confianza DECIMAL(5,4) COMMENT 'Nivel de confianza del modelo en la predicción',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_estudiante_tipo (estudiante_id, tipo_prediccion),
    INDEX idx_nivel_riesgo (nivel_riesgo),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB COMMENT='Historial de predicciones de IA sobre rendimiento académico';

-- ============================================
-- Vista: resumen_comportamiento_estudiante
-- Descripción: Vista consolidada del comportamiento de un estudiante
-- ============================================
CREATE VIEW resumen_comportamiento_estudiante AS
SELECT 
    e.estudiante_id,
    -- Asistencia
    COUNT(DISTINCT a.id) as total_dias_registrados,
    SUM(CASE WHEN a.presente = TRUE THEN 1 ELSE 0 END) as dias_presentes,
    ROUND(SUM(CASE WHEN a.presente = TRUE THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(DISTINCT a.id), 0), 2) as porcentaje_asistencia,
    
    -- Entregas
    COUNT(DISTINCT et.id) as total_tareas,
    SUM(CASE WHEN et.fecha_entrega <= et.fecha_limite THEN 1 ELSE 0 END) as tareas_a_tiempo,
    SUM(CASE WHEN et.fecha_entrega > et.fecha_limite THEN 1 ELSE 0 END) as tareas_tarde,
    SUM(CASE WHEN et.fecha_entrega IS NULL AND NOW() >= et.fecha_limite THEN 1 ELSE 0 END) as tareas_no_entregadas,
    AVG(CASE WHEN et.minutos_retraso > 0 THEN et.minutos_retraso ELSE NULL END) as promedio_minutos_retraso,
    
    -- Evaluaciones
    COUNT(DISTINCT ie.id) as total_intentos_evaluacion,
    SUM(CASE WHEN ie.es_correcta = TRUE THEN 1 ELSE 0 END) as respuestas_correctas,
    ROUND(AVG(ie.similitud_semantica), 4) as similitud_promedio,
    
    -- Actividad
    COUNT(DISTINCT la.id) as total_acciones,
    SUM(la.duracion_segundos) as tiempo_total_segundos,
    
    -- Última actividad
    MAX(la.timestamp) as ultima_actividad
    
FROM (SELECT DISTINCT estudiante_id FROM logs_actividad UNION SELECT DISTINCT estudiante_id FROM asistencias) e
LEFT JOIN asistencias a ON e.estudiante_id = a.estudiante_id
LEFT JOIN entregas_tareas et ON e.estudiante_id = et.estudiante_id
LEFT JOIN intentos_evaluacion ie ON e.estudiante_id = ie.estudiante_id
LEFT JOIN logs_actividad la ON e.estudiante_id = la.estudiante_id
GROUP BY e.estudiante_id;

-- ============================================
-- Usuario para la aplicación (solo permisos en rep_ia)
-- ============================================
CREATE USER IF NOT EXISTS 'rep_ia_user'@'localhost' IDENTIFIED BY 'rep_ia_2024';
GRANT SELECT, INSERT, UPDATE, DELETE ON rep_ia.* TO 'rep_ia_user'@'localhost';
FLUSH PRIVILEGES;

-- Nota: La aplicación Spring Boot puede usar credenciales admin/admin 
-- para desarrollo, pero en producción debería usar rep_ia_user
