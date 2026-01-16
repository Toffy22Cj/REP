#!/usr/bin/env zsh
# ==========================================
# EMPAQUETADOR LINUX/MAC - SISTEMA EDUCATIVO
# Versión: 1.0.0 - Compatible con ZSH
# ==========================================

# Colores para terminal (ZSH compatible)
autoload -U colors && colors
RED='%F{red}'
GREEN='%F{green}'
YELLOW='%F{yellow}'
BLUE='%F{blue}'
CYAN='%F{cyan}'
MAGENTA='%F{magenta}'
NC='%f'

echo ""
echo "=========================================="
echo " EMPAQUETADOR LINUX/MAC - SISTEMA EDUCATIVO"
echo "=========================================="
echo ""

# [1/6] Verificar requisitos
print -P "${CYAN}[1/6]${NC} Verificando requisitos..."
if ! command -v mvn &> /dev/null; then
    print -P "${RED}ERROR: Maven no está instalado${NC}"
    echo "Por favor, instale Maven:"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  Fedora: sudo dnf install maven"
    echo "  macOS: brew install maven"
    echo "  Arch: sudo pacman -S maven"
    exit 1
fi
print -P "   ${GREEN}✓${NC} Maven encontrado: $(mvn -version | head -n 1)"
echo ""

# [2/6] Limpiar compilaciones anteriores
print -P "${CYAN}[2/6]${NC} Limpiando compilaciones anteriores..."
rm -rf target dist 2>/dev/null
mkdir -p dist
print -P "   ${GREEN}✓${NC} Carpetas limpiadas"
echo ""

# [3/6] Compilar proyecto
print -P "${CYAN}[3/6]${NC} Compilando proyecto con Maven..."
echo "   Esto puede tomar varios minutos..."
mvn clean package -DskipTests
if [[ $? -ne 0 ]]; then
    echo ""
    print -P "${RED}ERROR: La compilación falló${NC}"
    echo "Revise los mensajes de error anteriores."
    exit 1
fi
print -P "   ${GREEN}✓${NC} Compilación exitosa"
echo ""

# [4/6] Crear estructura de carpetas
print -P "${CYAN}[4/6]${NC} Creando estructura de carpetas..."
mkdir -p "dist/Sistema Educativo REP"
mkdir -p "dist/Sistema Educativo REP/data"
mkdir -p "dist/Sistema Educativo REP/logs"
mkdir -p "dist/Sistema Educativo REP/backups"
print -P "   ${GREEN}✓${NC} Estructura creada"
echo ""

# [5/6] Copiar archivos
print -P "${CYAN}[5/6]${NC} Copiando archivos..."

# Buscar JAR compilado (más flexible)
JAR_FILES=(target/*.jar(N))
if [[ ${#JAR_FILES[@]} -eq 0 ]]; then
    print -P "${RED}ERROR: No se encontraron archivos JAR en target/${NC}"
    ls -la target/
    exit 1
fi

# Seleccionar el JAR principal (preferencia por standalone)
SOURCE_JAR=""
for jar in $JAR_FILES; do
    case ${jar:t} in
        *standalone*|*app*|*REP*|*main*)
            SOURCE_JAR=$jar
            break
            ;;
    esac
done

# Si no se encontró específico, usar el primero
if [[ -z $SOURCE_JAR ]]; then
    SOURCE_JAR=$JAR_FILES[1]
fi

cp "$SOURCE_JAR" "dist/Sistema Educativo REP/app.jar"
print -P "   ${GREEN}✓${NC} JAR copiado: app.jar (de: ${SOURCE_JAR:t})"

# Copiar archivos opcionales
[[ -f "README.md" ]] && cp "README.md" "dist/Sistema Educativo REP/" && print -P "   ${GREEN}✓${NC} README copiado"
[[ -f "LICENSE" ]] && cp "LICENSE" "dist/Sistema Educativo REP/" && print -P "   ${GREEN}✓${NC} LICENSE copiado"
[[ -f "colegio.png" ]] && cp "colegio.png" "dist/Sistema Educativo REP/" && print -P "   ${GREEN}✓${NC} Icono copiado"
[[ -f "icon.ico" ]] && cp "icon.ico" "dist/Sistema Educativo REP/" && print -P "   ${GREEN}✓${NC} Icono ICO copiado"

# Crear archivo de configuración
cat > "dist/Sistema Educativo REP/application.properties" << 'EOF'
# Configuración standalone
spring.profiles.active=standalone
server.port=18080
server.address=127.0.0.1

# Base de datos H2 embebida
spring.datasource.url=jdbc:h2:file:./data/sistema_educativo;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
EOF
print -P "   ${GREEN}✓${NC} Configuración creada"
echo ""

# [6/6] Crear scripts de ejecución
print -P "${CYAN}[6/6]${NC} Creando scripts de ejecución..."

# ===== Script ZSH para Linux/Mac =====
cat > "dist/Sistema Educativo REP/iniciar.zsh" << 'EOF'
#!/usr/bin/env zsh
# Sistema Educativo REP - Launcher Script (ZSH)
# Versión: 1.0.0

# Colores
autoload -U colors && colors
RED='%F{red}'
GREEN='%F{green}'
YELLOW='%F{yellow}'
CYAN='%F{cyan}'
BLUE='%F{blue}'
NC='%f'

clear
print -P "${CYAN}=========================================${NC}"
print -P "${CYAN}  SISTEMA EDUCATIVO REP - STANDALONE    ${NC}"
print -P "${CYAN}  Versión 1.0.0                         ${NC}"
print -P "${CYAN}=========================================${NC}"
echo ""

# Verificar Java
print -P "${YELLOW}Verificando Java...${NC}"
if ! command -v java &> /dev/null; then
    print -P "${RED}ERROR: Java no está instalado.${NC}"
    echo ""
    echo "Por favor, instale Java 17 o superior:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  Fedora/RHEL: sudo dnf install java-17-openjdk"
    echo "  macOS: brew install openjdk@17"
    echo "  Arch: sudo pacman -S jdk17-openjdk"
    echo ""
    echo "O descargue desde: https://adoptium.net/"
    echo ""
    vared -p "Presione Enter para salir... " -c dummy
    exit 1
fi

# Verificar versión de Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ $JAVA_VERSION -lt 17 ]]; then
    print -P "${RED}ERROR: Se requiere Java 17 o superior.${NC}"
    echo "Versión actual: Java $JAVA_VERSION"
    echo ""
    vared -p "Presione Enter para salir... " -c dummy
    exit 1
fi

print -P "${GREEN}✓ Java encontrado: $(java -version 2>&1 | head -n 1)${NC}"
echo ""
print -P "${GREEN}Iniciando aplicación...${NC}"
echo ""

# Crear carpetas necesarias si no existen
mkdir -p data logs backups

# Ejecutar aplicación
print -P "${BLUE}Ejecutando: java -jar \"app.jar\"${NC}"
echo ""
java -jar "app.jar"
EXIT_CODE=$?

# Al finalizar
echo ""
if [[ $EXIT_CODE -eq 0 ]]; then
    print -P "${GREEN}✓ Aplicación finalizada correctamente${NC}"
else
    print -P "${RED}✗ La aplicación terminó con código de error: $EXIT_CODE${NC}"
    echo "Revise los logs en: logs/application.log"
fi
echo ""
vared -p "Presione Enter para salir... " -c dummy
EOF

chmod +x "dist/Sistema Educativo REP/iniciar.zsh"
print -P "   ${GREEN}✓${NC} iniciar.zsh creado"

# ===== Script Bash compatible =====
cat > "dist/Sistema Educativo REP/iniciar.sh" << 'EOF'
#!/bin/bash
# Sistema Educativo REP - Launcher Script (Bash compatible)
# Versión: 1.0.0

# Colores (compatible con bash)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m'

clear
echo -e "${CYAN}=========================================${NC}"
echo -e "${CYAN}  SISTEMA EDUCATIVO REP - STANDALONE    ${NC}"
echo -e "${CYAN}  Versión 1.0.0                         ${NC}"
echo -e "${CYAN}=========================================${NC}"
echo ""

# Verificar Java
echo -e "${YELLOW}Verificando Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java no está instalado.${NC}"
    echo ""
    echo "Por favor, instale Java 17 o superior:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  Fedora/RHEL: sudo dnf install java-17-openjdk"
    echo "  macOS: brew install openjdk@17"
    echo "  Arch: sudo pacman -S jdk17-openjdk"
    echo ""
    echo "O descargue desde: https://adoptium.net/"
    echo ""
    read -p "Presione Enter para salir... "
    exit 1
fi

# Verificar versión de Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}ERROR: Se requiere Java 17 o superior.${NC}"
    echo "Versión actual: Java $JAVA_VERSION"
    echo ""
    read -p "Presione Enter para salir... "
    exit 1
fi

echo -e "${GREEN}✓ Java encontrado: $(java -version 2>&1 | head -n 1)${NC}"
echo ""
echo -e "${GREEN}Iniciando aplicación...${NC}"
echo ""

# Crear carpetas necesarias si no existen
mkdir -p data logs backups

# Ejecutar aplicación
echo -e "${BLUE}Ejecutando: java -jar \"app.jar\"${NC}"
echo ""
java -jar "app.jar"
EXIT_CODE=$?

# Al finalizar
echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Aplicación finalizada correctamente${NC}"
else
    echo -e "${RED}✗ La aplicación terminó con código de error: $EXIT_CODE${NC}"
    echo "Revise los logs en: logs/application.log"
fi
echo ""
read -p "Presione Enter para salir... "
EOF

chmod +x "dist/Sistema Educativo REP/iniciar.sh"
print -P "   ${GREEN}✓${NC} iniciar.sh creado (Bash compatible)"

# ===== Crear archivo .desktop para Linux =====
if [[ "$(uname)" == "Linux" ]]; then
    cat > "dist/Sistema Educativo REP/sistema-educativo.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=Sistema Educativo REP
Comment=Sistema de gestión educativa standalone
Exec=zsh -c 'cd "%k/.." && ./iniciar.zsh'
Icon=applications-education
Terminal=true
Categories=Education;Office;
StartupNotify=true
Keywords=education;school;management;java
EOF
    chmod +x "dist/Sistema Educativo REP/sistema-educativo.desktop"
    print -P "   ${GREEN}✓${NC} .desktop creado (Linux)"
fi

# ===== Script para desarrolladores =====
cat > "dist/Sistema Educativo REP/desarrollo.zsh" << 'EOF'
#!/usr/bin/env zsh
# Script para modo desarrollo

autoload -U colors && colors
GREEN='%F{green}'
YELLOW='%F{yellow}'
NC='%f'

print -P "${YELLOW}=== MODO DESARROLLO ===${NC}"
echo "Base de datos: H2 en memoria"
echo "Puerto: 18081"
echo "Datos: NO persistentes (se pierden al cerrar)"
echo ""

# Crear configuración temporal
cat > temp-dev.properties << 'DEVCFG'
spring.datasource.url=jdbc:h2:mem:devdb
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
server.port=18081
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
logging.level.com.rep=DEBUG
DEVCFG

print -P "${GREEN}Iniciando en modo desarrollo...${NC}"
echo "Consola H2: http://localhost:18081/h2-console"
echo ""

java -jar "app.jar" --spring.config.location=temp-dev.properties

# Limpiar
rm -f temp-dev.properties 2>/dev/null
echo ""
print -P "${GREEN}Modo desarrollo finalizado${NC}"
EOF

chmod +x "dist/Sistema Educativo REP/desarrollo.zsh"
print -P "   ${GREEN}✓${NC} desarrollo.zsh creado"

# ===== Crear README =====
cat > "dist/Sistema Educativo REP/README.txt" << 'EOF'
SISTEMA EDUCATIVO REP
=====================
Versión Standalone 1.0.0

DESCRIPCIÓN
-----------
Sistema de gestión educativa completo que funciona sin
necesidad de instalar MySQL, MariaDB o cualquier otro
servidor de base de datos.

CÓMO INICIAR
------------
1. Extraiga esta carpeta donde desee
2. Abra terminal en esta carpeta
3. Ejecutar uno de los siguientes:

   Para ZSH (recomendado):
     chmod +x iniciar.zsh
     ./iniciar.zsh

   Para Bash:
     chmod +x iniciar.sh
     ./iniciar.sh

   Para Linux (con interfaz):
     Doble clic en sistema-educativo.desktop

   Modo desarrollo (datos temporales):
     ./desarrollo.zsh

REQUISITOS
----------
- Java 17 o superior (OpenJDK o Oracle)
- ZSH o Bash (ZSH recomendado para mejores colores)
- Linux, macOS, o WSL2 en Windows

CARPETAS
--------
data/      - Base de datos y archivos persistentes
logs/      - Registros de la aplicación
backups/   - Copias de seguridad automáticas

ACCESOS RÁPIDOS
---------------
- Aplicación: Se abre automáticamente
- Consola H2: http://localhost:18080/h2-console
  Usuario: sa / Contraseña: (vacía)
- API Docs: http://localhost:18080/swagger-ui.html
- Logs: ./logs/application.log

SOLUCIÓN DE PROBLEMAS
---------------------
1. Si no inicia: Verifique Java 17+ instalado
2. Si puerto ocupado: java -jar app.jar --server.port=18082
3. Para actualizar: Reemplace app.jar, NO toque data/
4. Para backup: Copie toda la carpeta "data"

CONTACTO
--------
GitHub: https://github.com/Toffy22Cj/REP
Issues: https://github.com/Toffy22Cj/REP/issues

Distribuido bajo licencia: $(cat LICENSE 2>/dev/null || echo "Ver archivo LICENSE")
EOF
print -P "   ${GREEN}✓${NC} README creado"
echo ""

# ===== Informe final =====
print -P "${MAGENTA}==========================================${NC}"
print -P "${MAGENTA}  ¡EMPAQUETADO COMPLETADO EXITOSAMENTE!  ${NC}"
print -P "${MAGENTA}==========================================${NC}"
echo ""

print -P "${CYAN}RESULTADO:${NC}"
echo "Carpeta creada: dist/Sistema Educativo REP"
echo ""
echo "Contenido:"
ls -la "dist/Sistema Educativo REP/" | awk 'NR>1 {print "  " $9 " (" $5 ")"}'
echo ""

# Tamaño del JAR
JAR_SIZE=$(du -h "dist/Sistema Educativo REP/app.jar" | cut -f1)
print -P "${YELLOW}Tamaño del archivo principal: $JAR_SIZE${NC}"

# Tamaño total
TOTAL_SIZE=$(du -sh "dist/Sistema Educativo REP" | cut -f1)
print -P "${YELLOW}Tamaño total de la carpeta: $TOTAL_SIZE${NC}"
echo ""

# Instrucciones
print -P "${GREEN}INSTRUCCIONES PARA DISTRIBUIR:${NC}"
echo "1. Comprimir la carpeta:"
echo "   tar -czf Sistema_Educativo_REP.tar.gz 'dist/Sistema Educativo REP/'"
echo "2. O crear ZIP:"
echo "   zip -r Sistema_Educativo_REP.zip 'dist/Sistema Educativo REP/'"
echo "3. Nombre sugerido:"
echo "   Sistema_Educativo_REP_Standalone_$(date +%Y%m%d).zip"
echo ""
print -P "${GREEN}PARA LOS USUARIOS:${NC}"
echo "Solo necesitan extraer y ejecutar: ./iniciar.zsh"
echo ""

# Preguntar por compresión
vared -p "${YELLOW}¿Desea comprimir automáticamente? (s/N): ${NC}" -c COMPRESS
if [[ $COMPRESS == "s" || $COMPRESS == "S" ]]; then
    echo ""
    print -P "${CYAN}Comprimiendo...${NC}"
    cd dist
    tar -czf "Sistema_Educativo_REP_$(date +%Y%m%d_%H%M).tar.gz" "Sistema Educativo REP/"
    print -P "${GREEN}✓ ZIP creado: $(ls -la Sistema_Educativo_REP_*.tar.gz | tail -1 | awk '{print $9}')${NC}"
    cd ..
fi

echo ""
vared -p "${YELLOW}¿Desea probar la aplicación ahora? (s/N): ${NC}" -c TEST
if [[ $TEST == "s" || $TEST == "S" ]]; then
    echo ""
    print -P "${CYAN}Iniciando prueba...${NC}"
    cd "dist/Sistema Educativo REP"
    ./iniciar.zsh
    cd ../..
else
    echo ""
    print -P "${GREEN}¡Listo! La aplicación está empaquetada en:${NC}"
    print -P "${BLUE}  dist/Sistema Educativo REP/${NC}"
    echo ""
    print -P "${YELLOW}Puede abrir la carpeta con:${NC}"
    echo "  open 'dist/Sistema Educativo REP' (macOS)"
    echo "  xdg-open 'dist/Sistema Educativo REP' (Linux)"
    echo "  explorer.exe 'dist\Sistema Educativo REP' (WSL)"
fi
