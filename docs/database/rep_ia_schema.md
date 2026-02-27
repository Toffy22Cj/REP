# REP-IA: Documentación del Esquema de Base de Datos

## Base de Datos: `rep_ia`

Base de datos separada para almacenar datos de sensores de comportamiento y predicciones de IA.

---

## Tablas

### 1. `asistencias`

**Propósito**: Registro diario de asistencia de estudiantes por materia.

**Campos**:
- `id` (PK): ID autoincremental
- `estudiante_id`: FK a `rep.usuarios.id`
- `materia_id`: FK a `rep.materias.id`
- `fecha`: Fecha de la clase
- `presente`: Boolean (TRUE/FALSE)
- `observaciones`: Texto opcional (ej: "Justificado - Cita médica")
- `created_at`, `updated_at`: Timestamps automáticos

**Índices**:
- `idx_estudiante_fecha`: Para consultas por estudiante
- `idx_materia_fecha`: Para consultas por materia
- `idx_unico_asistencia`: UNIQUE constraint para evitar duplicados

**Casos de uso**:
- Calcular porcentaje de asistencia
- Detectar patrones de ausentismo
- Predicción de riesgo por baja asistencia

---

### 2. `entregas_tareas`

**Propósito**: Registro de entregas de tareas con cálculo automático de retrasos.

**Campos**:
- `id` (PK): ID autoincremental
- `estudiante_id`: FK a `rep.usuarios.id`
- `materia_id`: FK a `rep.materias.id`
- `actividad_id`: FK a `rep.actividades.id` (nullable)
- `titulo`: Nombre de la tarea
- `fecha_limite`: Deadline de entrega
- `fecha_entrega`: Fecha real de entrega (NULL si no entregó)
- `minutos_retraso` (COMPUTED): Minutos de retraso calculados automáticamente
- `estado` (COMPUTED): PENDIENTE | ENTREGADA_A_TIEMPO | ENTREGADA_TARDE | NO_ENTREGADA

**Columnas Computadas**:
```sql
minutos_retraso = TIMESTAMPDIFF(MINUTE, fecha_limite, fecha_entrega)
estado = calculado basado en fecha_entrega vs fecha_limite
```

**Casos de uso**:
- Medir cumplimiento de plazos
- Identificar estudiantes con problemas de organización
- Feature para modelo predictivo

---

### 3. `intentos_evaluacion`

**Propósito**: Almacenar intentos de respuesta para evaluación semántica.

**Campos**:
- `id` (PK): ID autoincremental
- `estudiante_id`: FK a `rep.usuarios.id`
- `pregunta_id`: FK a `rep.preguntas.id`
- `actividad_id`: FK a `rep.actividades.id` (nullable)
- `respuesta_texto`: Texto de la respuesta del estudiante
- `respuesta_correcta`: Texto de la respuesta esperada
- `similitud_semantica`: Valor 0.0000 - 1.0000
- `es_correcta`: Boolean
- `metodo_evaluacion`: EXACTA | SEMANTICA | PARCIAL | MANUAL

**Casos de uso**:
- Evaluar respuestas usando IA semántica
- Comparar "1600" vs "mil seiscientos"
- Entrenar modelos de evaluación

---

### 4. `logs_actividad`

**Propósito**: Logs de actividad del estudiante en el sistema.

**Campos**:
- `id` (PK): ID autoincremental
- `estudiante_id`: FK a `rep.usuarios.id`
- `accion`: LOGIN | LOGOUT | VER_MATERIA | RESPONDER_EVALUACION | etc.
- `materia_id`: FK opcional
- `actividad_id`: FK opcional
- `duracion_segundos`: Duración de la actividad
- `metadata`: JSON con datos adicionales

**Ejemplo de metadata**:
```json
{
  "ip": "192.168.1.100",
  "dispositivo": "Windows",
  "seccion": "Materiales"
}
```

**Casos de uso**:
- Medir tiempo de uso del sistema
- Detectar patrones de uso
- Identificar estudiantes con baja interacción

---

### 5. `historial_predicciones`

**Propósito**: Historial de predicciones de IA sobre estudiantes.

**Campos**:
- `id` (PK): ID autoincremental
- `estudiante_id`: FK a `rep.usuarios.id`
- `tipo_prediccion`: RIESGO_ACADEMICO | DESERCION | RENDIMIENTO
- `probabilidad_riesgo`: 0.0000 - 1.0000
- `nivel_riesgo`: BAJO | MEDIO | ALTO
- `features_usados`: JSON con features del modelo
- `factores_principales`: JSON array con factores de riesgo
- `recomendaciones`: JSON array con recomendaciones
- `modelo_version`: Versión del modelo (ej: "rf_v1.0")
- `confianza`: Nivel de confianza del modelo

**Ejemplo de JSON**:
```json
{
  "features_usados": {
    "promedio_notas": 2.8,
    "asistencia": 0.60,
    "tareas_tarde": 3
  },
  "factores_principales": [
    "Baja asistencia",
    "Múltiples entregas tardías"
  ],
  "recomendaciones": [
    "Considerar tutorías personalizadas",
    "Contactar con tutor"
  ]
}
```

**Casos de uso**:
- Guardar historial de predicciones
- Analizar evolución del riesgo
- Auditoría de decisiones de IA

---

## Vistas

### `resumen_comportamiento_estudiante`

**Propósito**: Vista consolidada del comportamiento de cada estudiante.

**Campos Calculados**:
- `total_dias_registrados`: Días con registro de asistencia
- `dias_presentes`: Días que asistió
- `porcentaje_asistencia`: Porcentaje de asistencia
- `total_tareas`: Total de tareas asignadas
- `tareas_a_tiempo`: Tareas entregadas a tiempo
- `tareas_tarde`: Tareas entregadas con retraso
- `tareas_no_entregadas`: Tareas no entregadas
- `promedio_minutos_retraso`: Promedio de minutos de retraso
- `total_intentos_evaluacion`: Intentos de respuesta
- `respuestas_correctas`: Respuestas correctas
- `similitud_promedio`: Similitud semántica promedio
- `total_acciones`: Total de acciones en logs
- `tiempo_total_segundos`: Tiempo total en el sistema
- `ultima_actividad`: Timestamp de última actividad

**Uso**:
```sql
SELECT * FROM resumen_comportamiento_estudiante WHERE estudiante_id = 1;
```

---

## Usuarios de Base de Datos

### Usuario de Aplicación
- **Usuario**: `rep_ia_user`
- **Password**: `rep_ia_2024`
- **Permisos**: SELECT, INSERT, UPDATE, DELETE en `rep_ia.*`

### Usuario Admin (Desarrollo)
- **Usuario**: `admin`
- **Password**: `admin`
- **Permisos**: ALL PRIVILEGES

---

## Configuración de Spring Boot

Añadir en `application.properties`:

```properties
# Datasource principal (REP)
spring.datasource.url=jdbc:mysql://localhost:3306/rep
spring.datasource.username=admin
spring.datasource.password=admin

# Datasource para IA
ia.datasource.url=jdbc:mysql://localhost:3306/rep_ia
ia.datasource.username=rep_ia_user
ia.datasource.password=rep_ia_2024
```

---

## Migraciones

- **V1_0_0__create_ia_database.sql**: Crea la BD y todas las tablas
- **V1_0_1__insert_sample_ia_data.sql**: Inserta datos de prueba

**Ejecutar migraciones**:
```bash
mysql -u admin -padmin < src/main/resources/db/migration/V1_0_0__create_ia_database.sql
mysql -u admin -padmin < src/main/resources/db/migration/V1_0_1__insert_sample_ia_data.sql
```

---

## Próximos Pasos

1. Crear entidades JPA en Spring Boot
2. Configurar DataSource secundario para `rep_ia`
3. Crear repositorios JPA
4. Implementar servicios para capturar datos de sensores
5. Integrar con microservicio Python para predicciones
