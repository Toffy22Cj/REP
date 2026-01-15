# Guía de Releases y Actualizaciones🚀 Sistema de Actualizaciones Automát

icas - Guía para Desarrolladores

Este documento describe cómo crear releases y activar el sistema de actualizaciones automáticas.

## 📋 Requisitos Previos

1. **Git** tag configurado correctamente
2. **Acceso** al repositorio GitHub
3. **Version** actualizada en el código

## 🎯 Proceso de Release

### 1.Actualizar la Versión

Editar `src/main/resources/version.json`:
```json
{
  "version": "1.1.0",  // <-- Incrementar aquí
  "build_date": "2026-01-14T19:32:00Z",
  ...
}
```

### 2. Commit y Push

```bash
# Hacer commit de los cambios
git add .
git commit -m "Release v1.1.0 - Descripción de cambios"
git push origin main
```

### 3. Crear Tag

```bash
# Crear tag anotado (importante: debe empezar con 'v')
git tag -a v1.1.0 -m "Release version 1.1.0

Cambios principales:
- Nueva funcionalidad X
- Corrección de bug Y
- Mejora de rendimiento Z
"

# Push del tag (esto activa GitHub Actions)
git push origin v1.1.0
```

### 4. GitHub Actions se Ejecuta Automáticamente

El workflow `.github/workflows/release.yml` se activa y:

1. ✅ Compila el proyecto con Maven
2. ✅ Crea el paquete standalone
3. ✅ Genera scripts de inicio
4. ✅ Crea el ZIP de distribución
5. ✅ Publica el GitHub Release
6. ✅ Actualiza `latest-version.json`

### 5. Verificar el Release

1. Ir a https://github.com/Toffy22Cj/REP/releases
2. Verificar que el release v1.1.0 existe
3. Confirmar que el ZIP está disponible
4. Probar descarga manual

## 🔄 Cómo Funcionan las Actualizaciones

### Para los Usuarios

1. **Verificación Automática**:
   - La app verifica cada 7 días si hay actualizaciones
   - Se ejecuta en segundo plano sin bloquear
   
2. **Notificación**:
   - Si hay actualización, se crea archivo `update.available`
   - La UI puede mostrar una notificación
   
3. **Descarga Manual**:
   - El usuario descarga el ZIP desde GitHub
   - Reemplaza el archivo `app.jar`
   - Mantiene carpeta `data/` intacta

### API de GitHub Utilizada

```
GET https://api.github.com/repos/Toffy22Cj/REP/releases/latest
```

**Respuesta** (simplificada):
```json
{
  "tag_name": "v1.1.0",
  "name": "Release v1.1.0",
  "body": "Notas del release...",
  "assets": [
    {
      "name": "sistema-educativo-v1.1.0.zip",
      "browser_download_url": "https://github.com/Toffy22Cj/REP/releases/download/v1.1.0/sistema-educativo-v1.1.0.zip"
    }
  ]
}
```

## 🛠️ Troubleshooting

### Problema: El workflow no se ejecuta

**Solución**:
1. Verificar que el tag empiece con 'v'
2. Confirmar que el archivo `.github/workflows/release.yml` existe
3. Revisar pestaña "Actions" en GitHub

### Problema: Build falla

**Solución**:
1. Verificar logs en GitHub Actions
2. Probar compilación local: `mvn clean package -DskipTests`
3. Asegurar que todas las dependencias estén en pom.xml

### Problema: Límite de API de GitHub

**Solución**:
- GitHub permite 60 requests/hora sin autenticación
- Con token: 5000 requests/hora
- El servicio cachea resultados automáticamente

## 📊 Versionado Semántico

Usar [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH

Ejemplo: 1.2.3
```

- **MAJOR**: Cambios incompatibles en la API
- **MINOR**: Nueva funcionalidad compatible
- **PATCH**: Correcciones de bugs

### Ejemplos

```bash
# Bug fix
v1.0.1 -> v1.0.2

# Nueva funcionalidad
v1.0.2 -> v1.1.0

# Cambio incompatible
v1.1.0 -> v2.0.0
```

## 📝 Template de Release Notes

```markdown
## 🎉 Novedades

- Nueva funcionalidad de exportación mejorada
- Soporte para múltiples idiomas

## 🐛 Correcciones

- Solucionado error en cálculo de promedios
- Mejorado rendimiento de consultas

## ⚠️  Cambios Importantes

- Se requiere Java 17 o superior
- Migración automática de base de datos

## 📚 Documentación

- [Guía de Usuario](link)
- [FAQ](link)
```

## 🔐 Seguridad y Mejores Prácticas

### Buenas Prácticas

✅ **Siempre** probar localmente antes de crear tag  
✅ **Documentar** cambios en el commit del tag  
✅ **Incrementar** versión en version.json  
✅ **Verificar** que el ZIP contiene todos los archivos  
✅ **Probar** el paquete en un PC limpio  

### Evitar

❌ Crear tags sin compilar localmente  
❌ Olvidar incrementar la versión  
❌ Pushear tags con errores de compilación  
❌ Reemplazar tags ya publicados  

## 🚀 Flujo Completo de Ejemplo

```bash
# 1. Desarrollar nueva funcionalidad
git checkout -b feature/nueva-funcionalidad
# ... codificar ...
git commit -am "Implementar nueva funcionalidad"

# 2. Merge a main
git checkout main
git merge feature/nueva-funcionalidad

# 3. Actualizar versión
# Editar src/main/resources/version.json
# Cambiar "1.0.0" -> "1.1.0"

# 4. Commit de versión
git commit -am "Bump version to 1.1.0"
git push origin main

# 5. Crear y pushear tag
git tag -a v1.1.0 -m "Release 1.1.0 - Nueva funcionalidad agregada"
git push origin v1.1.0

# 6. GitHub Actions construye y publica automáticamente

# 7. Verificar release
# Ir a https://github.com/Toffy22Cj/REP/releases/latest

# 8. Notificar a usuarios
# (opcional) Enviar email, actualizar web, etc.
```

## 📞 Soporte

- **Issues**: https://github.com/Toffy22Cj/REP/issues
- **Discussions**: https://github.com/Toffy22Cj/REP/discussions

---

**Última actualización**: 2026-01-14  
**Versión de este documento**: 1.0
