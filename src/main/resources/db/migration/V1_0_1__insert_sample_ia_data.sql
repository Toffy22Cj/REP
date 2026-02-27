-- ============================================
-- REP 2.0: Datos de Ejemplo para Testing
-- Versión: 1.0.1
-- Descripción: Insertará algunos datos de prueba para validar el esquema
-- ============================================

USE rep_ia;

-- Nota: Estos datos asumen que ya existen estudiantes con IDs 1, 2, 3
-- y materias con IDs 1, 2 en la base de datos principal `rep`

-- ============================================
-- Datos de ejemplo: asistencias
-- ============================================
INSERT INTO asistencias (estudiante_id, materia_id, fecha, presente, observaciones) VALUES
-- Estudiante 1: Buena asistencia
(1, 1, DATE_SUB(CURDATE(), INTERVAL 10 DAY), TRUE, NULL),
(1, 1, DATE_SUB(CURDATE(), INTERVAL 9 DAY), TRUE, NULL),
(1, 1, DATE_SUB(CURDATE(), INTERVAL 8 DAY), TRUE, NULL),
(1, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), FALSE, 'Justificado - Cita médica'),
(1, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TRUE, NULL),

-- Estudiante 2: Asistencia irregular
(2, 1, DATE_SUB(CURDATE(), INTERVAL 10 DAY), TRUE, NULL),
(2, 1, DATE_SUB(CURDATE(), INTERVAL 9 DAY), FALSE, NULL),
(2, 1, DATE_SUB(CURDATE(), INTERVAL 8 DAY), FALSE, NULL),
(2, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TRUE, NULL),
(2, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), FALSE, 'Sin justificación');

-- ============================================
-- Datos de ejemplo: entregas_tareas
-- ============================================
INSERT INTO entregas_tareas (estudiante_id, materia_id, actividad_id, titulo, fecha_limite, fecha_entrega) VALUES
-- Estudiante 1: Puntual
(1, 1, NULL, 'Taller de Matemáticas 1', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 1, NULL, 'Taller de Matemáticas 2', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Estudiante 2: Con retrasos
(2, 1, NULL, 'Taller de Matemáticas 1', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 1, NULL, 'Taller de Matemáticas 2', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),

-- Estudiante 3: Mezcla
(3, 1, NULL, 'Taller de Matemáticas 1', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 1, NULL, 'Taller de Matemáticas 2', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- Datos de ejemplo: intentos_evaluacion
-- ============================================
INSERT INTO intentos_evaluacion (estudiante_id, pregunta_id, actividad_id, respuesta_texto, respuesta_correcta, similitud_semantica, es_correcta, metodo_evaluacion) VALUES
-- Evaluación exacta
(1, 1, NULL, '1600', '1600', 1.0000, TRUE, 'EXACTA'),
-- Evaluación semántica
(1, 2, NULL, 'mil seiscientos', '1600', 0.9200, TRUE, 'SEMANTICA'),
-- Respuesta incorrecta
(2, 1, NULL, '1500', '1600', 0.4500, FALSE, 'EXACTA'),
-- Respuesta conceptualmente correcta
(2, 2, NULL, 'La fotosíntesis es el proceso donde las plantas producen su alimento', 'Proceso de síntesis de glucosa en plantas', 0.8800, TRUE, 'SEMANTICA');

-- ============================================
-- Datos de ejemplo: logs_actividad
-- ============================================
INSERT INTO logs_actividad (estudiante_id, accion, materia_id, actividad_id, duracion_segundos, metadata) VALUES
(1, 'LOGIN', NULL, NULL, NULL, JSON_OBJECT('ip', '192.168.1.100', 'dispositivo', 'Windows')),
(1, 'VER_MATERIA', 1, NULL, 300, JSON_OBJECT('seccion', 'Materiales')),
(1, 'RESPONDER_EVALUACION', 1, 1, 600, JSON_OBJECT('preguntas_respondidas', 5)),
(1, 'LOGOUT', NULL, NULL, NULL, JSON_OBJECT('duracion_sesion', 900)),

(2, 'LOGIN', NULL, NULL, NULL, JSON_OBJECT('ip', '192.168.1.101', 'dispositivo', 'Android')),
(2, 'VER_MATERIA', 1, NULL, 120, JSON_OBJECT('seccion', 'Actividades')),
(2, 'LOGOUT', NULL, NULL, NULL, JSON_OBJECT('duracion_sesion', 120));

-- ============================================
-- Datos de ejemplo: historial_predicciones
-- ============================================
INSERT INTO historial_predicciones (
    estudiante_id, 
    tipo_prediccion, 
    probabilidad_riesgo, 
    nivel_riesgo, 
    features_usados,
    factores_principales,
    recomendaciones,
    modelo_version,
    confianza
) VALUES
(1, 'RIESGO_ACADEMICO', 0.1500, 'BAJO', 
    JSON_OBJECT('promedio_notas', 4.2, 'asistencia', 0.95, 'tareas_tarde', 0),
    JSON_ARRAY('Buen rendimiento general', 'Alta asistencia'),
    JSON_ARRAY('Mantener el buen desempeño'),
    'rf_v1.0',
    0.9200
),
(2, 'RIESGO_ACADEMICO', 0.7200, 'ALTO', 
    JSON_OBJECT('promedio_notas', 2.8, 'asistencia', 0.60, 'tareas_tarde', 3),
    JSON_ARRAY('Baja asistencia', 'Múltiples entregas tardías', 'Promedio bajo'),
    JSON_ARRAY('Considerar tutorías personalizadas', 'Contactar con tutor', 'Reforzar hábitos de organización'),
    'rf_v1.0',
    0.8800
);
