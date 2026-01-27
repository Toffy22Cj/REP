# 🛡️ PLAN FASE 2: Unificación, Limpieza y Seguridad Total

Este documento unifica todas las recomendaciones de seguridad previas, el análisis de `COMENZAR_AQUI.txt` y el plan de desmantelamiento de JavaFX en una única hoja de ruta detallada.

---

## 🚀 PASOS PRELIMINARES (Acción Inmediata)
*Antes de iniciar cualquier modificación, establecer la línea base.*

1.  **Verificación Inicial:** Ejecutar `bash check-security.sh` para documentar las 13 vulnerabilidades actuales.
2.  **Preparación del Entorno:**
    ```bash
    cp production.env.example .env.local
    chmod 600 .env.local
    echo ".env.local" >> .gitignore
    echo "application-prod.properties" >> .gitignore
    ```
3.  **Generación de Clave Maestra:**
    ```bash
    openssl rand -base64 64
    # Copiar el resultado a JWT_SECRET en .env.local
    ```

---

## 🛑 PARTE 1: Limpieza Criogénica de JavaFX
*Objetivo: Eliminar rastros de UI del backend y convertirlo en un microservicio puro.*

### 1.1 Cirugía de Dependencias (`pom.xml`)
- [ ] **Eliminar** JavaFX: `javafx-controls`, `javafx-fxml`.
- [ ] **Eliminar** plugin: `javafx-maven-plugin`.
- [ ] **Agregar** dependencias de seguridad recomendadas en `SECURITY_DEPENDENCIES.xml`:
    - `bucket4j-core` (Rate Limiting)
    - `jasypt-spring-boot-starter` (Encriptación de propiedades)
    - `spring-boot-starter-validation` (Validación de entrada)

### 1.2 Eliminación de Código Legado
- [ ] **Borrar** `src/main/java/com/rep/MainFx.java`.
- [ ] **Borrar** paquete `com.rep.controller.views` (11 controladores de escritorio).
- [ ] **Borrar** paquete `com.rep.service.fx` (NavigationService, etc.).
- [ ] **Borrar** `src/main/java/com/rep/config/SpringFXMLLoader.java`.

### 1.3 Refactorización de `Main.java`
- [ ] Eliminar `extends Application` y métodos `init()`, `start()`, `stop()`.
- [ ] Dejar un `main` de Spring Boot estándar: `SpringApplication.run(Main.class, args)`.

### 1.4 Limpieza de Recursos Físicos
- [ ] **Borrar** carpeta `src/main/resources/view/` (FXMLs).
- [ ] **Borrar** carpeta `src/main/resources/Styles/` (CSS).

---

## 🔒 PARTE 2: Endurecimiento de Blindaje (Seguridad)
*Basado en RECOMENDACIONES_SEGURIDAD.md y PLAN_IMPLEMENTACION.md.*

### 2.1 Gestión de Secretos
- [ ] Mover credenciales de BD y JWT a `.env.local`.
- [ ] Configurar `application-prod.properties` para usar estas variables `${DB_USERNAME}`, etc.

### 2.2 Defensa Activa (Rate Limiting)
- [ ] Integrar `RateLimitFilter.java` en el proyecto.
- [ ] Configurar 5 intentos/15 min para login y 100 req/min para API general.

### 2.3 Saneamiento Perimetral
- [ ] **Validación Global:** Aplicar anotaciones de validación en todos los DTOs.
- [ ] **Manejo de Errores:** Implementar `GlobalExceptionHandler` para evitar la fuga de stack traces.
- [ ] **Headers de Seguridad:** Integrar la configuración de `SecurityConfigSecure.java` (HSTS, CSP, etc.).

### 2.4 Limpieza de Logs
- [ ] Buscar y eliminar todos los `System.out.println` que expongan tokens o datos sensibles.
- [ ] Ajustar niveles de logging en `application-prod.properties`.

---

## 🛠️ PARTE 3: Infraestructura y Auditoría

### 3.1 Base de Datos Segura
- [ ] Cambiar `ddl-auto=update` a `validate` en el perfil de producción.
- [ ] Asegurar conexión forzada via SSL (`useSSL=true`).

### 3.2 Auditoría Activa
- [ ] Implementar la entidad `AuditLog` para registrar eventos críticos de seguridad.

---

## 📅 Hoja de Ruta de Ejecución

1.  **Día 1: Baseline y Limpieza.** Ejecutar script de seguridad → Borrar JavaFX → Lograr compilación exitosa.
2.  **Día 2: Blindaje de Credenciales.** Externalizar secretos a variables de entorno → Habilitar SSL.
3.  **Día 3: Control de Flujo.** Implementar Rate Limiting y Validación de entrada.
4.  **Día 4: Fortaleza de Headers.** Configurar Headers HTTP y Exception Handling global.
5.  **Día 5: Verificación Final.** Ejecutar `bash check-security.sh` (Objetivo: 92/100).

---

> [!CAUTION]
> **Plan de Rollback:** Realizar un commit de git o respaldo de la carpeta `src/` antes de ejecutar la eliminación masiva de la Parte 1.
