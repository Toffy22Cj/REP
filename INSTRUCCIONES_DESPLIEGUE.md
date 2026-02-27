# INSTRUCCIONES DE DESPLIEGUE - UI Admin v2.0

## 📦 Pre-requisitos

- Java 21 (JDK 21)
- Maven 3.8+
- Base de datos configurada (MySQL/PostgreSQL)
- Navegador moderno (Chrome, Firefox, Edge)

---

## 🔧 Pasos de Despliegue

### 1. Verificar que los archivos estén en su lugar

```bash
# Navegar al directorio del proyecto
cd /home/carlos/Proyectos/REP

# Verificar archivos JS modificados
ls -la src/main/resources/static/admin/js/modules/
```

**Archivos esperados:**

- ✅ usuarios.js (modificado)
- ✅ academico.js (modificado)
- ✅ auditoria.js (modificado)
- ✅ api.js (sin cambios)
- ✅ ui.js (sin cambios)

### 2. Compilar el proyecto

```bash
# Limpiar compilaciones anteriores
mvn clean

# Compilar el proyecto
mvn compile

# Empaquetar
mvn package -DskipTests

# O si quieres correr los tests:
mvn package
```

### 3. Ejecutar la aplicación

```bash
# Opción 1: Desde Maven
mvn spring-boot:run

# Opción 2: Ejecutar el JAR
java -jar target/rep-application.jar

# Opción 3: Usando el script incluido
./mvnw spring-boot:run
```

### 4. Acceder a la interfaz

```
URL: http://localhost:8080/admin
Usuario: admin@local.test
(Contraseña según tu configuración)
```

---

## ✅ Verificación Post-Despliegue

### 1. Verificar que la interfaz carga

1. Abrir navegador: `http://localhost:8080/admin`
2. Iniciar sesión
3. Debe cargar el Dashboard

### 2. Verificar módulo de Usuarios

1. Ir a "Usuarios" en el menú lateral
2. Deben aparecer:
   - Botón "+ Nuevo Usuario"
   - Filtros de búsqueda
   - Tabla de usuarios con botones:
     - Detalles ✅ NUEVO
     - Editar ✅ NUEVO
     - Activar/Desactivar ✅ NUEVO
     - Eliminar ✅ NUEVO

### 3. Verificar módulo Académico

1. Ir a "Académico"
2. Deben aparecer secciones:
   - Cursos con botones: Estudiantes, Editar ✅ NUEVO, Eliminar ✅ NUEVO
   - Materias con botones: Editar ✅ NUEVO, Eliminar ✅ NUEVO
   - Asignaciones ✅ NUEVA SECCIÓN
     - Crear asignación ✅ NUEVO
     - Ver asignaciones
     - Eliminar asignación

### 4. Verificar módulo Auditoría

1. Ir a "Auditoría"
2. Deben aparecer:
   - Estadísticas en vivo ✅ NUEVO (Usuarios, Estudiantes, Profesores, Cursos, Materias)
   - Tabla de logs
   - Filtro por acción ✅ NUEVO
   - Búsqueda de logs

---

## 🐛 Solución de Problemas

### Problema: "Error al cargar módulo"

**Solución:**

1. Abrir consola del navegador (F12)
2. Verificar que no haya errores de red (404)
3. Limpiar caché del navegador (Ctrl+Shift+Supr)
4. Reiniciar el servidor

### Problema: "Botones no funcionan"

**Solución:**

1. Verificar que los módulos tengan `window.moduloModule = Modulo` al final
2. Verificar la consola por errores JavaScript
3. Verificar que los archivos .js se carguen correctamente (Network tab)

### Problema: "No aparecen datos en tabla"

**Solución:**

1. Verificar que el servidor esté corriendo
2. Verificar que la base de datos esté accesible
3. Verificar logs del servidor: `tail -f logs/application.log`
4. Verificar en Network tab si las peticiones API devuelven datos

### Problema: "Modal no se abre"

**Solución:**

1. Verificar CSS: `src/main/resources/static/admin/css/styles.css`
2. Abrir consola y buscar errores JavaScript
3. Verificar que ui.js esté cargándose correctamente

---

## 📊 Verificación de Endpoints

Para verificar que los endpoints estén disponibles:

```bash
# Listar usuarios
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/usuarios

# Listar cursos
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/cursos

# Listar materias
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/materias

# Listar auditoría
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/auditoria

# Obtener estadísticas
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/stats
```

---

## 🔒 Configuración de Seguridad

Verificar que el archivo `application.properties` contiene:

```properties
# Seguridad
spring.security.user.name=admin
spring.security.user.password=<contraseña_segura>

# JWT
app.jwt.secret=<secret_key_larga>
app.jwt.expiration=86400000

# CORS para admin panel
cors.allowed-origins=http://localhost:8080
```

---

## 📈 Monitoreo Post-Despliegue

### Logs que debes verificar

```bash
# Ver logs en tiempo real
tail -f logs/application.log | grep -E "ERROR|WARN|INFO"

# Contar errores
grep -c "ERROR" logs/application.log

# Ver últimos 100 logs
tail -100 logs/application.log
```

### Métricas a Monitorear

- Tiempo de respuesta de APIs
- Número de usuarios activos
- Errores en la consola del navegador
- Logs de auditoría

---

## 🔄 Rollback (si es necesario)

Si algo falla, puedes revertir:

```bash
# Restaurar archivos desde git
git checkout src/main/resources/static/admin/js/modules/

# Recompilar versión anterior
mvn clean package -DskipTests

# Reiniciar servidor
```

---

## 📋 Checklist de Despliegue

- [ ] Todos los archivos .js están en su lugar
- [ ] El proyecto compila sin errores
- [ ] La aplicación inicia correctamente
- [ ] Se puede acceder a http://localhost:8080/admin
- [ ] Se puede iniciar sesión
- [ ] El módulo de Usuarios funciona
- [ ] El módulo de Académico funciona
- [ ] El módulo de Auditoría funciona
- [ ] Los botones nuevo "Editar", "Detalles", "Eliminar" funcionan
- [ ] Los filtros funcionan
- [ ] Las alertas de éxito/error aparecen
- [ ] Los logs de auditoría se registran
- [ ] No hay errores en la consola (F12)

---

## 📞 Contacto y Soporte

En caso de problemas:

1. Verificar GUIA_PRUEBAS_UI_ADMIN.md
2. Revisar logs del servidor
3. Verificar consola del navegador (F12)
4. Consultar CAMBIOS_UI_ADMIN_v2.md para detalles técnicos

---

## 📝 Nota Importante

**Esta es una versión ESTABLE y LISTA PARA PRODUCCIÓN.**

Se han completado todas las funcionalidades solicitadas:

- ✅ Editar usuarios
- ✅ Cambiar estado de usuarios
- ✅ Ver detalles de usuario
- ✅ Eliminar usuarios
- ✅ Editar cursos y materias
- ✅ Ver estudiantes por curso
- ✅ Gestionar asignaciones
- ✅ Auditoría mejorada

---

**Versión**: 2.0  
**Fecha**: 27 de Enero de 2026  
**Estado**: ✅ Listo para Producción

Disfruta de la nueva versión mejorada del Panel de Administración REP.
