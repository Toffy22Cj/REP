
# 🛡️ PLAN FASE 2: Unificación, Limpieza, Seguridad Total y Migración del Admin Web

Este documento unifica todas las recomendaciones de seguridad previas, el análisis de `COMENZAR_AQUI.txt`, el plan de desmantelamiento de JavaFX y la **migración completa del módulo Admin a Web/PWA**, en una única hoja de ruta clara, segura y ejecutable para un entorno **offline (LAN)**.

---

## 🚀 PASOS PRELIMINARES (Acción Inmediata)

*Antes de iniciar cualquier modificación, establecer una línea base segura y reversible.*

1. **Snapshot obligatorio del proyecto**

   ```bash
   git checkout -b fase-2-headless-admin
   git tag pre-fase-2
   ```

2. **Verificación Inicial**

   ```bash
   bash check-security.sh > security-baseline.txt
   ```

3. **Preparación del Entorno Seguro**

   ```bash
   cp production.env.example .env.prod
   chmod 600 .env.prod
   echo ".env.prod" >> .gitignore
   echo "application-prod.properties" >> .gitignore
   ```

4. **Generación de Clave Maestra JWT**

   ```bash
   openssl rand -base64 64
   # Copiar el resultado a JWT_SECRET en .env.prod
   ```

---

## 🛑 PARTE 1: Limpieza Criogénica de JavaFX

*Objetivo: eliminar completamente la UI del backend y convertirlo en un servicio headless.*

### 1.1 Cirugía de Dependencias (`pom.xml`)

* [ ] **Eliminar** `javafx-controls`
* [ ] **Eliminar** `javafx-fxml`
* [ ] **Eliminar** `javafx-maven-plugin`
* [ ] **Agregar**:

  * `spring-boot-starter-validation`
  * `bucket4j-core`
  * `jasypt-spring-boot-starter`

---

### 1.2 Eliminación Progresiva de Código Legado

> ⚠️ No eliminar todo de una sola vez

**Desactivar primero**

* [ ] Eliminar referencias JavaFX activas
* [ ] Asegurar que ningún `@Autowired` dependa de `service.fx`

**Eliminar definitivamente**

* [ ] `src/main/java/com/rep/MainFx.java`
* [ ] `src/main/java/com/rep/config/SpringFXMLLoader.java`
* [ ] `src/main/java/com/rep/controller/views/**`
* [ ] `src/main/java/com/rep/service/fx/**`

🧪 Verificación tras cada bloque:

```bash
mvn clean package
```

---

### 1.3 Refactorización de `Main.java`

* [ ] Eliminar `extends Application`
* [ ] Eliminar `init()`, `start()`, `stop()`
* [ ] Dejar únicamente:

  ```java
  SpringApplication.run(Main.class, args);
  ```

---

### 1.4 Limpieza de Recursos

* [ ] **Mover primero**:

  * `src/main/resources/view/` → `legacy/javafx/view`
  * `src/main/resources/Styles/` → `legacy/javafx/styles`
* [ ] **Eliminar definitivamente** tras verificación

---

## 🔒 PARTE 2: Endurecimiento de Blindaje (Seguridad)

*Seguridad fuerte, ajustada a entorno LAN sin Internet.*

---

### 2.1 Gestión de Secretos

* [ ] Mover credenciales BD, JWT y rutas sensibles a `.env.prod`
* [ ] Usar `${VAR_NAME}` en `application-prod.properties`
* [ ] Limitar permisos de lectura solo al usuario del servicio

---

### 2.2 Defensa Activa (Rate Limiting)

* [ ] Integrar `RateLimitFilter.java`
* [ ] Configurar límites:

  * `/auth/login` → 5 intentos / 15 min
  * `/auth/refresh` → 10 / hora
  * `/api/**` → 120 req / min
  * `/admin/**` → 60 req / min

---

### 2.3 Saneamiento Perimetral

* [ ] Aplicar validaciones `@Valid` en todos los DTOs
* [ ] Implementar `GlobalExceptionHandler`
* [ ] Evitar fuga de stack traces en producción

---

### 2.4 Headers de Seguridad HTTP

* [ ] `X-Content-Type-Options`
* [ ] `X-Frame-Options`
* [ ] `Referrer-Policy`
* [ ] CSP básica (compatible con Admin Web y Qt)

---

### 2.5 Limpieza de Logs

* [ ] Eliminar `System.out.println`
* [ ] No registrar tokens, passwords ni payloads completos
* [ ] Ajustar niveles:

  * `INFO` negocio
  * `WARN` eventos sospechosos
  * `ERROR` fallos reales

---

## 🌐 PARTE 3: Migración del Admin a Web / PWA (Reemplazo de JavaFX)

*Objetivo: eliminar JavaFX y centralizar la administración vía Web Local Offline.*

---

### 3.1 Arquitectura del Admin

```
Spring Boot
 ├── /api/**        → clientes Qt/QML
 ├── /admin/api/**  → backend del admin
 └── /admin/**      → frontend Web / PWA
```

* [ ] Todo servido desde el mismo JAR
* [ ] Sin nginx
* [ ] Sin dependencia de Internet

---

### 3.2 Tecnología del Admin

* [ ] HTML + CSS + JavaScript puro
* [ ] Fetch API
* [ ] Service Worker (PWA)
* [ ] ❌ No React / Angular

---

### 3.3 Funcionalidades del Admin Web

* [ ] Login ADMIN
* [ ] CRUD:

  * Profesores
  * Estudiantes
  * Cursos
  * Materias
* [ ] Gestión de archivos
* [ ] Backup / Restore
* [ ] Estado del sistema
* [ ] Logs básicos

---

### 3.4 Seguridad del Admin

* [ ] Acceso solo con rol `ADMIN`
* [ ] Autenticación por sesión (cookies HttpOnly)
* [ ] Acceso permitido solo desde LAN / localhost
* [ ] Rate limit independiente
* [ ] ❌ No JWT en frontend admin

---

### 3.5 PWA OFFLINE

* [ ] Cache de HTML, CSS, JS
* [ ] Funciona sin conexión
* [ ] Instalación opcional
* [ ] No cachear endpoints sensibles

---

## 🛠️ PARTE 4: Infraestructura y Auditoría

### 4.1 Base de Datos

* [ ] Cambiar `ddl-auto=update` → `validate`
* [ ] Forzar SSL si la BD está en otra máquina

---

### 4.2 Auditoría Activa

* [ ] Implementar entidad `AuditLog`
* [ ] Registrar:

  * Login / logout
  * CRUD crítico
  * Descargas de archivos
  * Cambios de rol

---

🎯 **Objetivo:** 85–92 / 100 en `check-security.sh`

---

> [!CAUTION]
> **Plan de Rollback:**
>
> ```bash
> git reset --hard pre-fase-2
> ```
>
> Recuperación inmediata del sistema ante cualquier fallo.
