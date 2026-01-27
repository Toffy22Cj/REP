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
# Crear variables de entorno
cp production.env.example .env.local
nano .env.local  # Editar con valores reales
chmod 600 .env.local

# Cargar variables
./setup-env.sh
```

### 3. Compilar

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
Usuario: admin
Contraseña: (la configurada)
```

---

## 📖 Documentación

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
