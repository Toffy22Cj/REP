# Guía de Pruebas - UI Admin v2.0

## Requisitos Previos

- El servidor debe estar ejecutándose
- Debe haber iniciado sesión como administrador
- Base de datos debe estar funcionando

---

## 1. PRUEBAS DE USUARIOS

### 1.1 Crear un Nuevo Usuario

**Pasos:**

1. Ir a la sección "Usuarios" en el menú
2. Clic en "+ Nuevo Usuario"
3. Llenar formulario:
   - Nombres: `Juan`
   - Apellidos: `Pérez`
   - Correo: `juan.perez@example.com`
   - Tipo ID: `CC`
   - Identificación: `1234567890`
   - Edad: `25`
   - Sexo: `Masculino`
   - Rol: `Profesor`
   - Contraseña: `MiPassword123!`
4. Clic en "Guardar"

**Resultado esperado:**

- Mensaje de éxito "Usuario creado exitosamente"
- Usuario aparece en la tabla

### 1.2 Ver Detalles del Usuario

**Pasos:**

1. En la tabla de usuarios, clic en botón "Detalles" del usuario creado
2. Se abre un panel con la información del usuario

**Resultado esperado:**

- Se muestran: ID, Nombre, Apellido, Correo, Rol, Identificación

### 1.3 Editar Usuario

**Pasos:**

1. Clic en botón "Editar" del usuario
2. En el modal se cargan los datos actuales
3. Cambiar el correo a: `juan.modificado@example.com`
4. Cambiar rol a: `ESTUDIANTE`
5. Clic en "Guardar"

**Resultado esperado:**

- Mensaje de éxito
- Tabla se actualiza con los cambios

### 1.4 Desactivar Usuario

**Pasos:**

1. En la tabla, clic en "Desactivar" (botón rojo) del usuario
2. Confirmar en el diálogo

**Resultado esperado:**

- Usuario se marca como inactivo
- Botón cambia a "Activar"
- Usuario aparece filtrado cuando se selecciona "Inactivo"

### 1.5 Activar Usuario

**Pasos:**

1. Con el usuario desactivado, clic en "Activar"
2. Confirmar

**Resultado esperado:**

- Usuario vuelve a estar activo
- Botón vuelve a mostrar "Desactivar"

### 1.6 Eliminar Usuario

**Pasos:**

1. Clic en botón "Eliminar" del usuario
2. Confirmar en el diálogo de advertencia

**Resultado esperado:**

- Usuario se elimina de la base de datos
- Desaparece de la tabla
- Mensaje de confirmación

### 1.7 Filtrado de Usuarios

**Pasos:**

1. En la barra de búsqueda, escribir: `juan`
2. En filtro "Rol", seleccionar: `Profesor`
3. En filtro "Estado", seleccionar: `Activo`

**Resultado esperado:**

- Tabla se filtra en tiempo real
- Solo muestra usuarios que coinciden con los criterios

---

## 2. PRUEBAS DE ACADÉMICO

### 2.1 Crear un Nuevo Curso

**Pasos:**

1. Ir a "Académico"
2. En sección "Cursos", clic en "+ Nuevo Curso"
3. Llenar:
   - Grado: `10`
   - Grupo: `A`
4. Clic en "Guardar"

**Resultado esperado:**

- Mensaje de éxito
- Curso aparece en tabla como "Grado 10 - Grupo A"

### 2.2 Ver Estudiantes del Curso

**Pasos:**

1. Clic en "Estudiantes" del curso creado
2. Se carga vista con lista de estudiantes

**Resultado esperado:**

- Si no hay estudiantes, muestra "No se encontraron registros"
- Si hay estudiantes, muestra tabla con: ID, Nombre, Apellido, Identificación, Correo

### 2.3 Editar Curso

**Pasos:**

1. Clic en "Editar" del curso
2. Cambiar Grupo a: `B`
3. Clic en "Guardar"

**Resultado esperado:**

- Curso se actualiza a "Grado 10 - Grupo B"

### 2.4 Eliminar Curso

**Pasos:**

1. Clic en "Eliminar" del curso
2. Confirmar

**Resultado esperado:**

- Curso se elimina si no tiene estudiantes asignados
- Si tiene estudiantes, muestra error: "No se puede eliminar el curso porque tiene estudiantes asignados"

### 2.5 Crear Nueva Materia

**Pasos:**

1. En sección "Materias", clic en "+ Nueva Materia"
2. Nombre: `Matemáticas Avanzadas`
3. Clic en "Guardar"

**Resultado esperado:**

- Materia aparece en tabla

### 2.6 Editar Materia

**Pasos:**

1. Clic en "Editar" de la materia
2. Cambiar nombre a: `Cálculo Diferencial`
3. Clic en "Guardar"

**Resultado esperado:**

- Materia se actualiza

### 2.7 Eliminar Materia

**Pasos:**

1. Clic en "Eliminar" de la materia
2. Confirmar

**Resultado esperado:**

- Si no tiene asignaciones, se elimina
- Si tiene asignaciones, muestra error: "No se puede eliminar la materia porque está asignada a uno o más profesores"

### 2.8 Crear Asignación (Profesor-Materia-Curso)

**Pasos:**

1. En sección "Asignaciones", clic en "+ Nueva Asignación"
2. Seleccionar:
   - Profesor: (seleccionar uno disponible)
   - Curso: (seleccionar uno disponible)
   - Materia: (seleccionar una disponible)
3. Clic en "Guardar"

**Resultado esperado:**

- Asignación aparece en tabla
- Muestra nombre del profesor, curso y materia

### 2.9 Eliminar Asignación

**Pasos:**

1. Clic en "Eliminar" de la asignación
2. Confirmar

**Resultado esperado:**

- Asignación se elimina

---

## 3. PRUEBAS DE AUDITORÍA

### 3.1 Verificar Estadísticas

**Pasos:**

1. Ir a "Auditoría"
2. Observar los cuadros de estadísticas superiores

**Resultado esperado:**

- Se muestran contadores actualizados de:
  - Usuarios
  - Estudiantes
  - Profesores
  - Cursos
  - Materias

### 3.2 Ver Logs de Auditoría

**Pasos:**

1. En la tabla de auditoría, se muestran todos los registros
2. Cada fila contiene: Fecha/Hora, Usuario, Acción, Detalles

**Resultado esperado:**

- Se ven registros de todas las acciones anteriores realizadas
- Ejemplos: CREAR_USUARIO, ACTUALIZAR_USUARIO, CREAR_CURSO, etc.

### 3.3 Buscar en Auditoría

**Pasos:**

1. En la barra de búsqueda, escribir: `USUARIO`
2. Se filtra en tiempo real

**Resultado esperado:**

- Solo muestra acciones relacionadas con usuarios

### 3.4 Filtrar por Acción

**Pasos:**

1. En filtro "Filtrar por Acción", seleccionar: `CREAR_USUARIO`
2. Se filtra la tabla

**Resultado esperado:**

- Solo muestra logs donde la acción es "CREAR_USUARIO"

### 3.5 Combinación de Filtros

**Pasos:**

1. Búsqueda: `admin`
2. Filtro Acción: `CAMBIAR_ESTADO_USUARIO`

**Resultado esperado:**

- Tabla muestra solo logs que coincidan con ambos criterios

---

## 4. PRUEBAS DE INTEGRACIÓN

### 4.1 Flujo Completo de Profesor

**Pasos:**

1. Crear usuario (Profesor)
2. Crear curso
3. Crear materia
4. Crear asignación (profesor-materia-curso)
5. Ver logs en auditoría

**Resultado esperado:**

- Todos los pasos se ejecutan sin errores
- Se generan logs de cada acción

### 4.2 Restricciones de Eliminación

**Pasos:**

1. Crear profesor
2. Crear curso
3. Crear materia
4. Asignar profesor a materia-curso
5. Intentar eliminar profesor
6. Intentar eliminar materia
7. Intentar eliminar curso

**Resultado esperado:**

- Profesor: Se elimina si no tiene asignaciones, error si las tiene
- Materia: Error si tiene asignaciones
- Curso: Error si tiene estudiantes o asignaciones

---

## 5. PRUEBAS DE VALIDACIÓN

### 5.1 Validación de Correo Duplicado

**Pasos:**

1. Crear usuario con correo `test@example.com`
2. Intentar crear otro usuario con el mismo correo

**Resultado esperado:**

- Error: "Error: El correo ya existe"

### 5.2 Validación de Identificación Duplicada

**Pasos:**

1. Crear usuario con identificación `1234567890`
2. Intentar crear otro usuario con la misma identificación

**Resultado esperado:**

- Error: "Error: La identificación ya existe"

### 5.3 Validación de Edad vs Tipo Identificación

**Pasos:**

1. Crear usuario con:
   - Edad: `15`
   - Tipo ID: `CC` (Cédula)

**Resultado esperado:**

- Error: "No se permite Cédula (CC) para menores de edad"

---

## Notas Importantes

- Los tiempos de respuesta dependen del servidor
- Algunos errores pueden variar según el estado de la base de datos
- Limpiar la caché del navegador si hay problemas de carga
- Verificar la consola del navegador (F12) para más detalles de errores

---

**Guía creada**: 27 de Enero de 2026
