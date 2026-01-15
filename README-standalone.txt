╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║      SISTEMA EDUCATIVO REP - VERSIÓN STANDALONE 1.0.0       ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

📦 VERSIÓN TODO-EN-UNO
──────────────────────────────────────────────────────────────
¡Esta versión NO necesita instalar NADA más!

✓ Sin MySQL/MariaDB
✓ Sin configuración de base de datos
✓ Sin servidores externos
✓ 100% Portable - Copia y funciona

═══════════════════════════════════════════════════════════════

🚀 CÓMO INICIAR LA APLICACIÓN
──────────────────────────────────────────────────────────────

┌─ EN WINDOWS ─────────────────────────────────────────────┐
│                                                           │
│  1. Extraiga la carpeta "Sistema Educativo" donde desee  │
│  2. Doble clic en "Iniciar.bat"                          │
│  3. ¡La aplicación se abrirá automáticamente!            │
│                                                           │
│  Alternativa PowerShell:                                  │
│  - Clic derecho en "Iniciar.ps1"                         │
│  - Seleccionar "Ejecutar con PowerShell"                 │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ EN LINUX ───────────────────────────────────────────────┐
│                                                           │
│  1. Extraiga la carpeta "Sistema Educativo"              │
│  2. Abra una terminal en esa carpeta                     │
│  3. Ejecute: chmod +x iniciar.sh                         │
│  4. Ejecute: ./iniciar.sh                                │
│                                                           │
│  Alternativa (con interfaz gráfica):                     │
│  - Doble clic en "sistema-educativo.desktop"            │
│  - Seleccionar "Ejecutar" o "Lanzar"                    │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ EN MACOS ───────────────────────────────────────────────┐
│                                                           │
│  1. Extraiga la carpeta "Sistema Educativo"              │
│  2. Abra Terminal en esa carpeta                         │
│  3. Ejecute: chmod +x iniciar.sh                         │
│  4. Ejecute: ./iniciar.sh                                │
│                                                           │
└──────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

💾 BASE DE DATOS INCLUIDA
──────────────────────────────────────────────────────────────

Tipo: H2 Database (embebida)
Ubicación: ./data/sistema_educativo.mv.db
           (Se crea automáticamente al primer inicio)

┌─ ACCESO A LA CONSOLA H2 ────────────────────────────────┐
│                                                           │
│  URL del navegador:                                       │
│    http://localhost:18080/h2-console                     │
│                                                           │
│  Credenciales de acceso:                                  │
│    • JDBC URL: jdbc:h2:file:./data/sistema_educativo    │
│    • Usuario: sa                                          │
│    • Contraseña: (dejar vacío)                           │
│                                                           │
└──────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

🌐 SERVIDOR WEB INTEGRADO
──────────────────────────────────────────────────────────────

Puerto: 18080 (localhost)
Dirección: 127.0.0.1 (solo accesible localmente)

┌─ SERVICIOS DISPONIBLES ──────────────────────────────────┐
│                                                           │
│  • Interfaz principal: Se abre automáticamente           │
│  • API REST: http://localhost:18080/api/                │
│  • Documentación API: http://localhost:18080/swagger-ui.html
│  • Consola H2: http://localhost:18080/h2-console        │
│  • Health Check: http://localhost:18080/actuator/health │
│                                                           │
└──────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

📁 ESTRUCTURA DE CARPETAS
──────────────────────────────────────────────────────────────

Sistema Educativo/
├── app.jar                 ← Aplicación principal (~100-150 MB)
├── Iniciar.bat             ← Lanzador para Windows
├── Iniciar.ps1             ← Lanzador PowerShell (Windows)
├── iniciar.sh              ← Lanzador para Linux/Mac
├── sistema-educativo.desktop ← Integración Linux
├── README.txt              ← Este archivo
├── INFO.txt                ← Información rápida
├── colegio.png             ← Ícono (si está disponible)
│
├── data/                   ← Base de datos y archivos
│   └── sistema_educativo.mv.db  (Creado automáticamente)
│
├── logs/                   ← Registros de la aplicación
│   └── application.log     (Creado automáticamente)
│
└── backups/                ← Para copias de seguridad
                             (Uso manual)

═══════════════════════════════════════════════════════════════

📋 REQUISITOS DEL SISTEMA
──────────────────────────────────────────────────────────────

✓ Java 17 o superior (OBLIGATORIO)

┌─ VERIFICAR JAVA ─────────────────────────────────────────┐
│                                                           │
│  Windows:                                                 │
│    Abrir CMD y ejecutar: java -version                   │
│                                                           │
│  Linux/Mac:                                               │
│    Abrir Terminal y ejecutar: java -version              │
│                                                           │
│  Debe mostrar "version 17" o superior.                   │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ INSTALAR JAVA (SI ES NECESARIO) ────────────────────────┐
│                                                           │
│  Opción 1 - Eclipse Temurin (Recomendado):              │
│    https://adoptium.net/                                  │
│                                                           │
│  Opción 2 - Por sistema operativo:                       │
│    • Windows: Descargar desde adoptium.net               │
│    • Ubuntu/Debian: sudo apt install openjdk-17-jdk      │
│    • Fedora: sudo dnf install java-17-openjdk            │
│    • macOS: brew install openjdk@17                       │
│                                                           │
└──────────────────────────────────────────────────────────┘

Sistemas operativos soportados:
  • Windows 7, 8, 10, 11 (64-bit)
  • Linux (Ubuntu 18.04+, Fedora 30+, Debian 10+)
  • macOS 10.14 (Mojave) o superior

Hardware mínimo:
  • RAM: 2 GB (recomendado 4 GB)
  • Disco: 500 MB libres
  • Procesador: Dual-core 1.5 GHz o superior

═══════════════════════════════════════════════════════════════

🔧 SOLUCIÓN DE PROBLEMAS
──────────────────────────────────────────────────────────────

┌─ PROBLEMA: No inicia la aplicación ──────────────────────┐
│                                                           │
│  Solución 1: Verificar Java                              │
│    - Ejecutar: java -version                             │
│    - Debe mostrar versión 17+                            │
│                                                           │
│  Solución 2: Puerto ocupado                              │
│    - El puerto 18080 puede estar en uso                  │
│    - Cerrar otras aplicaciones                           │
│    - O editar application-standalone.properties          │
│                                                           │
│  Solución 3: Permisos (Linux/Mac)                        │
│    - Ejecutar: chmod +x iniciar.sh                       │
│    - Verificar permisos de escritura en data/ y logs/   │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ PROBLEMA: Error de base de datos ───────────────────────┐
│                                                           │
│  Solución:                                                │
│    1. Cerrar completamente la aplicación                 │
│    2. Eliminar la carpeta "data"                         │
│    3. Volver a iniciar (se recreará automáticamente)     │
│                                                           │
│  NOTA: Esto ELIMINARÁ todos los datos actuales.          │
│  Haga backup antes si tiene datos importantes.           │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ PROBLEMA: La interfaz no aparece ───────────────────────┐
│                                                           │
│  Solución:                                                │
│    - Esperar 10-15 segundos al primer inicio             │
│    - Revisar logs/application.log para errores          │
│    - Verificar que JavaFX esté incluido en su JDK        │
│                                                           │
└──────────────────────────────────────────────────────────┘

Para reiniciar limpiamente:
  1. Cerrar completamente la aplicación
  2. Esperar 10 segundos
  3. Volver a ejecutar Iniciar.bat (o iniciar.sh)

═══════════════════════════════════════════════════════════════

💾 COPIAS DE SEGURIDAD Y MIGRACIÓN
──────────────────────────────────────────────────────────────

┌─ HACER BACKUP ───────────────────────────────────────────┐
│                                                           │
│  1. Cerrar la aplicación completamente                   │
│  2. Copiar la carpeta "data" completa                    │
│  3. Guardar en lugar seguro (USB, nube, etc.)           │
│                                                           │
│  Importante: Solo copiar cuando la app esté CERRADA     │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ RESTAURAR BACKUP ───────────────────────────────────────┐
│                                                           │
│  1. Cerrar la aplicación                                 │
│  2. Eliminar la carpeta "data" actual                    │
│  3. Copiar la carpeta "data" del backup                  │
│  4. Iniciar la aplicación                                │
│                                                           │
└──────────────────────────────────────────────────────────┘

┌─ MIGRAR A OTRO PC ───────────────────────────────────────┐
│                                                           │
│  Opción A (Migración completa):                          │
│    - Copiar toda la carpeta "Sistema Educativo"          │
│    - Pegarla en el nuevo PC                              │
│    - Ejecutar normalmente                                │
│                                                           │
│  Opción B (Solo datos):                                  │
│    - Instalar app nueva en el nuevo PC                   │
│    - Copiar solo la carpeta "data"                       │
│    - Reemplazar en la instalación nueva                 │
│                                                           │
└──────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

🔄 ACTUALIZAR LA APLICACIÓN
──────────────────────────────────────────────────────────────

Para actualizar a una versión nueva:

1. Cerrar la aplicación actual
2. Hacer BACKUP de la carpeta "data"
3. Descargar la nueva versión
4. Reemplazar SOLO el archivo "app.jar"
5. Copiar de vuelta la carpeta "data" (si fue eliminada)
6. Iniciar normalmente

IMPORTANTE: NO eliminar la carpeta "data" al actualizar,
o perderá todos sus datos.

═══════════════════════════════════════════════════════════════

🔍 LOGS Y DIAGNÓSTICO
──────────────────────────────────────────────────────────────

Los registros de la aplicación se guardan en:
  logs/application.log

Para ver los logs en tiempo real:

  Windows:
    type logs\application.log

  Linux/Mac:
    tail -f logs/application.log

Los logs incluyen:
  - Errores de la aplicación
  - Consultas a la base de datos (en modo DEBUG)
  - Accesos a la API REST
  - Información de inicio/cierre

═══════════════════════════════════════════════════════════════

⚙️ CONFIGURACIÓN AVANZADA
──────────────────────────────────────────────────────────────

El archivo de configuración está integrado en app.jar, pero
puede ser sobrescrito creando un archivo externo:

  application-standalone.properties

Este archivo debe estar en la MISMA carpeta que app.jar.

Parámetros que puede cambiar:
  - server.port=18080           (Cambiar puerto)
  - logging.level.root=INFO     (Nivel de logs)
  - spring.jpa.show-sql=true    (Ver SQL en logs)

Consulte la documentación técnica para más opciones.

═══════════════════════════════════════════════════════════════

📞 INFORMACIÓN Y SOPORTE
──────────────────────────────────────────────────────────────

Nombre: Sistema Educativo REP
Versión: 1.0.0 Standalone
Fecha de compilación: Enero 2026

Base de datos: H2 2.2.224 (embebida)
Framework: Spring Boot 3.2.0 + JavaFX 21.0.2
Java requerido: 17+

Arquitectura: Fat JAR (todas las dependencias incluidas)
Tamaño aproximado: 100-150 MB

══════════════════════════════════════════════════════════════

⚠️  NOTAS IMPORTANTES
──────────────────────────────────────────────────────────────

✓ La aplicación se ejecuta LOCALMENTE en su PC
✓ NO se conecta a internet (salvo que usted lo configure)
✓ Todos los datos se guardan LOCALMENTE en ./data/
✓ Haga copias de seguridad periódicas de la carpeta "data"
✓ Para uso en red local, consulte documentación avanzada
✓ La consola H2 solo es accesible desde localhost
✓ El puerto 18080 debe estar libre para que funcione

══════════════════════════════════════════════════════════════

📄 LICENCIA Y TÉRMINOS DE USO
──────────────────────────────────────────────────────────────

Este software se distribuye tal cual, sin garantías.
Consulte el archivo LICENSE.txt para más información.

══════════════════════════════════════════════════════════════

✨ ¡GRACIAS POR USAR SISTEMA EDUCATIVO REP!

Si encuentra algún problema o necesita ayuda, consulte la
sección de Solución de Problemas de este documento.

══════════════════════════════════════════════════════════════
