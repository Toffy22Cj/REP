# 🚀 COMIENZA AQUÍ

Bienvenido al Sistema REP. Esta es tu guía para acceder a la documentación completa.

---

## 📋 ¿Qué necesitas?

### 👤 Soy Usuario Final / Administrativo
→ **[README.md](README.md)** (5 minutos)  
Guía rápida: instalación, uso, endpoints básicos.

### 👨‍💻 Soy Desarrollador
→ **[DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)** (Ver índice)  
- Secciones 1-3: Arquitectura (30 min)
- Secciones 4-6: Código y DB (45 min)
- Secciones 7-9: Config, API, UI (1 hora)
- Secciones 10-12: Operación y troubleshooting (30 min)

### 🔒 Soy DevOps / Infraestructura
→ **[DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)** Secciones:
- Sección 5: Base de Datos
- Sección 6: Seguridad
- Sección 7: Configuración
- Sección 11: Guía de Operación

### 🧪 Necesito Verificar Seguridad
```bash
bash check-security.sh
```
Luego revisar: **[DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)** Sección 6

---

## 📚 Estructura de Documentación

```
DOCUMENTACION_COMPLETA.md (3,094 líneas)
├─ 1. Visión General
├─ 2. Arquitectura del Sistema
├─ 3. Estructura del Proyecto
├─ 4. Componentes Técnicos
├─ 5. Base de Datos
├─ 6. Seguridad
├─ 7. Configuración
├─ 8. API REST
├─ 9. Interfaz de Usuario
├─ 10. Servicios y Dependencias
├─ 11. Guía de Operación
└─ 12. Troubleshooting
```

---

## ⚡ Inicio Rápido

```bash
# 1. Cargar variables de entorno
./setup-env.sh

# 2. Compilar
mvn clean package -DskipTests

# 3. Ejecutar
java -jar target/main-0.0.1-SNAPSHOT.jar

# 4. Acceder
# Abrir navegador: http://localhost:8080/admin/index.html
# Usuario: admin
# Contraseña: (la configurada en .env.local)
```

---

## 🔍 Buscar en Documentación

**Tabla de contenidos completa:** Ver [DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md) línea 1

**Búsquedas comunes:**
- JWT: Buscar "JWT" en sección 6 (Seguridad)
- Endpoints: Ver sección 8 (API REST)
- Base de datos: Ver sección 5
- Configuración: Ver sección 7
- Problemas: Ver sección 12 (Troubleshooting)

---

## 📞 Contacto Rápido

| Problema | Ubicación |
|----------|----------|
| No arranca la app | DOCUMENTACION_COMPLETA.md § 12.1 |
| Error de BD | DOCUMENTACION_COMPLETA.md § 12.2 |
| JWT inválido | DOCUMENTACION_COMPLETA.md § 12.1 |
| Puerto ocupado | DOCUMENTACION_COMPLETA.md § 12.1 |
| Verificar seguridad | `bash check-security.sh` |

---

**Versión:** 1.0 - 26 de enero de 2026  
**Próximo paso:** Abre [README.md](README.md) o [DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)
