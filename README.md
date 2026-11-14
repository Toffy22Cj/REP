# 📚 REP - Sistema Educativo

**Versión:** 1.0  
**Estado:** ✅ PRODUCCIÓN-READY  
**Documentación:** [DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)

---

## 🚀 Inicio Rápido

### 1. Requisitos

- Java 17 LTS
- MySQL 8.0
- Maven 3.8+

### 2. Configuración

```bash
cp .env.example .env
echo "JWT_SECRET=$(openssl rand -base64 48)" >> .env
nano .env   # credenciales de MySQL y, opcionalmente, ADMIN_INITIAL_PASSWORD
```

La aplicación lee `.env` automáticamente al arrancar (formato `CLAVE=valor`, sin
comillas). Las variables de entorno del sistema tienen prioridad. `.env` está en
`.gitignore`: nunca versione secretos.

### 2b. Datos de demostración (opcional)

Con `SEED_DEMO=true` en `.env` y una base de datos vacía, el primer arranque crea un
colegio de ejemplo (contraseña de todas las cuentas: `Demo12345`):

| Rol | Identificación |
|-----|----------------|
| Profesor Carlos Ramírez (Matemáticas, Física) | `1020304050` |
| Profesora Laura Gómez (Lengua, Inglés) | `1030405060` |
| Profesor Andrés Torres (Ciencias) | `1040506070` |
| Estudiantes 10-A | `1098765432` … `1098765437` |
| Estudiantes 11-A | `1098765438` … `1098765441` |

Incluye actividades (examen, quiz, taller) sin empezar, en curso, entregadas con nota,
pendientes de revisión, revisadas, vencidas e inactivas; preguntas con imagen; material de
estudio (PDF, PNG, TXT) y asistencia de la última semana. **No usar en producción.**

### Bloqueo por intentos de inicio de sesión

Solo cuentan los intentos **fallidos** (`LOGIN_MAX_INTENTOS`, por defecto 5, durante
`LOGIN_BLOQUEO_MINUTOS`, por defecto 15). El contador está en memoria: **reiniciar el
servidor desbloquea inmediatamente**.

### Periodos académicos y migraciones

- El administrador define los periodos del año (fechas y porcentaje) en el cliente de
  escritorio. Con periodos, las notas se consultan por periodo y la nota anual se
  pondera (Decreto 1290). Sin periodos, el cálculo es la media simple de siempre.
- El esquema se versiona con Flyway: ver [`docs/migraciones.md`](docs/migraciones.md).
  **Haga una copia de la base antes de actualizar** (`mysqldump colegio > copia.sql`).
- Informe de madurez y hoja de ruta: [`docs/PRODUCT_EVOLUTION_AND_MATURITY_REPORT.md`](docs/PRODUCT_EVOLUTION_AND_MATURITY_REPORT.md).

### Notas y funcionamiento sin internet

- `NOTA_MINIMA_APROBATORIA` (por defecto `3.0`) y `VENCIDAS_CUENTAN_CERO` (por defecto
  `true`: una actividad vencida sin entregar cuenta 0.0) ajustan el reporte de notas.
- El servidor no necesita internet: basta con una red local (LAN/Wi-Fi del colegio). Los
  clientes de escritorio guardan una copia de lo consultado y, si se pierde la conexión
  con el servidor, siguen mostrando notas, actividades y material, y encolan las
  entregas para enviarlas automáticamente al reconectar.

### 3. Compilar y probar

```bash
mvn test   # 92 pruebas (unitarias + integración de seguridad con H2)
```

### 3b. Compilar

```bash
mvn clean package -DskipTests
```

### 4. Ejecutar

```bash
java -jar target/main-0.0.1-SNAPSHOT.jar
# O con Maven: mvn spring-boot:run
```

### 5. Acceder

```
http://localhost:8080/admin/index.html
Usuario: 22
Contraseña: la de ADMIN_INITIAL_PASSWORD (o la generada que aparece en el log al primer arranque)
```

---

## 📖 Documentación

- Catálogo de errores de la API: [docs/errores.md](docs/errores.md)
- Historial de cambios: [CHANGELOG.md](CHANGELOG.md)

La **documentación completa** está en [DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)

Contiene:

- ✅ Arquitectura completa del sistema
- ✅ Estructura de directorios
- ✅ Base de datos (tablas, relaciones, restricciones)
- ✅ API REST (32 endpoints detallados)
- ✅ Seguridad (análisis, mitigaciones, configuración)
- ✅ Interfaz de usuario (HTML, CSS, JavaScript)
- ✅ Servicios y dependencias
- ✅ Guía de operación
- ✅ Troubleshooting

---

## 🎯 Funcionalidades Principales

### Panel de Administración (32 Endpoints)

| Módulo          | Funciones                      | Status |
| --------------- | ------------------------------ | ------ |
| 🔧 Cursos       | CRUD + listado estudiantes     | ✅     |
| 📚 Materias     | CRUD + filtros                 | ✅     |
| 👥 Usuarios     | CRUD + gestión roles           | ✅     |
| 🎓 Estudiantes  | Transferencias entre cursos    | ✅     |
| 👨‍🏫 Profesores   | Info + asignaciones + materias | ✅     |
| 📋 Asignaciones | Profesor-Materia-Curso         | ✅     |
| ⚙️ Sistema      | Login, Logout, Dashboard       | ✅     |

---

## 🔒 Seguridad

**Score:** 80/100 (MEDIA-ALTA)

### Implementado

✅ JWT authentication (256 bits)  
✅ BCrypt password encoding  
✅ CORS restringido a localhost  
✅ Rate limiting (5/15min login, 100/min general)  
✅ Variables de entorno protegidas  
✅ SQL injection prevention (JPA)  
✅ Validaciones servidor-side  
✅ Headers de seguridad

### Verificar

```bash
bash check-security.sh
```

---

## 📊 Stack Técnico

```
Frontend:  HTML5 / CSS3 / JavaScript ES6+
Backend:   Spring Boot 3.2.0 (Java 17)
DB:        MySQL 8.0 (InnoDB)
Auth:      JWT (JJWT 0.11.5)
Rate:      Bucket4j 8.1.1
Security:  Spring Security 6.2.0
```

---

## 📁 Estructura

```
REP/
├── src/main/java/com/rep/        [CÓDIGO FUENTE]
│   ├── controller/                [REST Endpoints]
│   ├── service/                   [Lógica negocio]
│   ├── repository/                [Acceso BD]
│   ├── model/                     [Entidades JPA]
│   └── security/                  [Autenticación]
│
├── src/main/resources/
│   ├── static/admin/              [INTERFAZ USUARIO]
│   │   ├── index.html
│   │   ├── css/styles.css
│   │   └── js/modules/
│   │
│   └── application-*.properties   [CONFIGURACIÓN]
│
├── pom.xml                        [DEPENDENCIAS]
├── .env.local                     [SECRETOS (NO SUBIR)]
├── DOCUMENTACION_COMPLETA.md      [📚 REFERENCIA]
└── check-security.sh              [🔒 VERIFICAR]
```

---

## 🛠️ Operaciones Comunes

### Desarrollo

```bash
./setup-env.sh
mvn spring-boot:run
```

### Producción

```bash
export spring.profiles.active=prod
java -jar target/main-*.jar
```

### Ver Logs

```bash
tail -f logs/application.log
```

### Backup BD

```bash
mysqldump -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME > backup.sql
```

### Verificar Salud

```bash
curl http://localhost:8080/actuator/health
```

---

## ⚠️ Checklist Importante

Antes de producción:

- [ ] Generar JWT_SECRET nuevo (`openssl rand -base64 64`)
- [ ] Configurar BD en servidor remoto
- [ ] Obtener certificado SSL/TLS (Let's Encrypt)
- [ ] Establecer variable `spring.profiles.active=prod`
- [ ] Ejecutar `check-security.sh` (score > 85)
- [ ] Hacer backup de BD
- [ ] Probar todos los 32 endpoints
- [ ] Revisar logs en busca de errores

---

## 📞 Soporte

Para problemas o dudas:

1. Revisar [DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)
2. Ver sección "12. TROUBLESHOOTING"
3. Ejecutar `bash check-security.sh`
4. Revisar `logs/application.log`

---

**Última actualización:** 26 de enero de 2026  
**Documentación versión:** 1.0 - COMPLETA
