# ⚙️ Módulo de Administración, UI Web y Control del Sistema

## 1. Propósito y Alcance

El módulo de **Administración y Operaciones** provee la consola de control unificada para rectores y coordinadores académicos. Integra una interfaz web ligera basada en estándares modernos sin dependencias externas pesadas, una bitácora forense de auditoría inmutable, y endpoints de control del ciclo de vida del servidor.

---

## 2. Arquitectura del Panel Web de Administración (Frontend)

El frontend administrativo reside en `src/main/resources/static/admin/` y está estructurado en módulos JavaScript nativos (ES6 Modules) servidos directamente por Spring Boot:

```
src/main/resources/static/admin/
├── css/
│   └── styles.css          # Sistema de diseño responsivo (Dark/Light tokens, tablas, modales)
├── js/
│   ├── auth.js             # Manejo de alertas de error en formulario de inicio de sesión
│   ├── main.js             # Enrutador cliente y control de barra lateral
│   └── modules/
│       ├── api.js          # Adaptador Fetch API con intercepción de sesiones vencidas
│       ├── ui.js           # Generador de componentes DOM (tablas, spinners, alertas)
│       ├── usuarios.js     # Control de usuarios, roles, estados y filtros
│       ├── academico.js    # Control de cursos, grupos y materias
│       └── auditoria.js    # Visualizador y buscador de la bitácora de auditoría
├── index.html              # Shell principal de la consola administrativa
└── login.html              # Interfaz de autenticación con formulario POST
```

### 2.1 Adaptador Fetch y Manejo de Sesión ([api.js](file:///home/carlos/Proyectos/REP/src/main/resources/static/admin/js/modules/api.js))
Encapsula las peticiones HTTP agregando cabeceras `Content-Type: application/json` y `Accept: application/json`. Si el servidor responde con estado `401 UNAUTHORIZED` o `403 FORBIDDEN` (por expiración de sesión), intercepta automáticamente el flujo y redirige al operador a `/admin/login.html`.

---

## 3. Bitácora Forense de Auditoría (`AuditLog`)

Cada acción crítica realizada desde el panel administrativo persiste un registro en la tabla `audit_logs` mediante la entidad [AuditLog.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/model/AuditLog.java):

```java
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;  // Identificación del usuario en sesión
    private String action;    // CREAR_CURSO, EDITAR_USUARIO, ELIMINAR_ASIGNACION, etc.
    private String details;   // Parámetros y entidades afectadas
    @CreatedDate
    private LocalDateTime timestamp;
}
```

### Consulta de Auditoría
El endpoint `GET /api/admin/auditoria` retorna la lista cronológica inversa de todos los eventos (`findAllByOrderByTimestampDesc()`). El módulo cliente `auditoria.js` permite filtrar dinámicamente en memoria por usuario, acción o texto del detalle.

---

## 4. Control del Ciclo de Vida del Servidor ([SystemApi.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/controller/apis/SystemApi.java))

`SystemApi` provee capacidades para supervisar y detener el servidor remotamente:

### 4.1 Estado del Sistema (`GET /api/admin/system/status`)
Retorna información básica de runtime de la Máquina Virtual Java:
- `uptime`: Milisegundos de arranque del sistema.
- `memory`: Memoria total reservada por la JVM (`Runtime.getRuntime().totalMemory()`).
- `status`: `"RUNNING"`.

### 4.2 Apagado Controlado (`POST /api/admin/system/shutdown`)
Para evitar que la conexión TCP se cierre bruscamente antes de que el cliente reciba la confirmación de apagado, el controlador ejecuta la finalización de Spring en un hilo asíncrono desacoplado con retardo de 1 segundo:
```java
@PostMapping("/shutdown")
public ResponseEntity<?> shutdown() {
    new Thread(() -> {
        try {
            Thread.sleep(1000);
            SpringApplication.exit(context, () -> 0);
            System.exit(0);
        } catch (Exception e) {
            log.error("Error durante el apagado: ", e);
        }
    }).start();

    return ResponseEntity.ok(Map.of(
            "message", "El servidor se apagará en un momento.",
            "status", "SHUTDOWN_INITIATED"));
}
```

---

## 5. Carga de Datos Inicial y Lanzador de Navegador

- **`DataLoader` ([DataLoader.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/config/DataLoader.java)):** Al iniciarse la aplicación, comprueba si existe un usuario con identificación `"22"`. Si no existe, genera el usuario administrador por defecto con rol `ADMIN`, correo `admin@local.test` y contraseña cifrada con BCrypt.
- **`BrowserLauncher` ([BrowserLauncher.java](file:///home/carlos/Proyectos/REP/src/main/java/com/rep/config/BrowserLauncher.java)):** Detecta si la JVM está operando en un entorno con pantalla gráfica. Si `java.awt.headless != true`, ejecuta la llamada del sistema operativo (`xdg-open` en Linux, `open` en macOS, `start` en Windows) para abrir la URL `http://localhost:8080/admin/` automáticamente en el navegador predeterminado del usuario.
