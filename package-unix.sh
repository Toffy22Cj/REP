#!/bin/bash
# ==========================================
# EMPAQUETADOR LINUX/MAC - SISTEMA EDUCATIVO
# Versión: 1.0.0
# ==========================================

# Colores para terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo ""
echo "=========================================="
echo " EMPAQUETADOR LINUX/MAC - SISTEMA EDUCATIVO"
echo "=========================================="
echo ""

# [1/6] Verificar requisitos
echo -e "${CYAN}[1/6]${NC} Verificando requisitos..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven no está instalado${NC}"
    echo "Por favor, instale Maven:"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  Fedora: sudo dnf install maven"
    echo "  macOS: brew install maven"
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Maven encontrado: $(mvn -version | head -n 1)"
echo ""

# [2/6] Limpiar compilaciones anteriores
echo -e "${CYAN}[2/6]${NC} Limpiando compilaciones anteriores..."
rm -rf target dist 2>/dev/null
mkdir -p dist
echo -e "   ${GREEN}✓${NC} Carpetas limpiadas"
echo ""

# [3/6] Compilar proyecto
echo -e "${CYAN}[3/6]${NC} Compilando proyecto con Maven..."
echo "   Esto puede tomar varios minutos..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}ERROR: La compilación falló${NC}"
    echo "Revise los mensajes de error anteriores."
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Compilación exitosa"
echo ""

# [4/6] Crear estructura de carpetas
echo -e "${CYAN}[4/6]${NC} Creando estructura de carpetas..."
mkdir -p "dist/Sistema Educativo/data"
mkdir -p "dist/Sistema Educativo/logs"
mkdir -p "dist/Sistema Educativo/backups"
echo -e "   ${GREEN}✓${NC} Estructura creada"
echo ""

# [5/6] Copiar archivos
echo -e "${CYAN}[5/6]${NC} Copiando archivos..."
if [ -f "target/sistema-educativo-standalone.jar" ]; then
    cp "target/sistema-educativo-standalone.jar" "dist/Sistema Educativo/app.jar"
    echo -e "   ${GREEN}✓${NC} JAR copiado: app.jar"
else
    echo -e "${RED}ERROR: No se encontró el JAR compilado${NC}"
    echo "Ubicación esperada: target/sistema-educativo-standalone.jar"
    exit 1
fi

# Copiar archivos opcionales
[ -f "README-standalone.txt" ] && cp "README-standalone.txt" "dist/Sistema Educativo/README.txt" && echo -e "   ${GREEN}✓${NC} README copiado"
[ -f "LICENSE.txt" ] && cp "LICENSE.txt" "dist/Sistema Educativo/" && echo -e "   ${GREEN}✓${NC} LICENSE copiado"
[ -f "colegio.png" ] && cp "colegio.png" "dist/Sistema Educativo/" && echo -e "   ${GREEN}✓${NC} Icono copiado"
echo ""

# [6/6] Crear scripts de ejecución
echo -e "${CYAN}[6/6]${NC} Creando scripts de ejecución..."

# ===== Script bash para Linux/Mac =====
cat > "dist/Sistema Educativo/iniciar.sh" << 'EOF'
#!/bin/bash
# Sistema Educativo REP - Launcher Script
# Versión: 1.0.0

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
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
    echo "  Fedora: sudo dnf install java-17-openjdk"
    echo "  macOS: brew install openjdk@17"
    echo ""
    echo "O descargue desde: https://adoptium.net/"
    echo ""
    read -p "Presione Enter para salir..."
    exit 1
fi

# Verificar versión de Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}ERROR: Se requiere Java 17 o superior.${NC}"
    echo "Versión actual: Java $JAVA_VERSION"
    echo ""
    read -p "Presione Enter para salir..."
    exit 1
fi

echo -e "${GREEN}✓ Java encontrado: $(java -version 2>&1 | head -n 1)${NC}"
echo ""
echo -e "${GREEN}Iniciando aplicación...${NC}"
echo ""

# Ejecutar aplicación
java -jar "app.jar"

# Al finalizar
echo ""
echo -e "${YELLOW}Aplicación finalizada${NC}"
read -p "Presione Enter para salir..."
EOF

chmod +x "dist/Sistema Educativo/iniciar.sh"
echo -e "   ${GREEN}✓${NC} iniciar.sh creado"

# ===== Crear archivo .desktop para Linux =====
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    cat > "dist/Sistema Educativo/sistema-educativo.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=Sistema Educativo REP
Comment=Sistema de gestión educativa standalone
Exec=bash -c 'cd "$(dirname "%k")" && ./iniciar.sh'
Icon=applications-education
Terminal=true
Categories=Education;Office;
StartupNotify=true
EOF
    chmod +x "dist/Sistema Educativo/sistema-educativo.desktop"
    echo -e "   ${GREEN}✓${NC} sistema-educativo.desktop creado (Linux)"
fi

# ===== Crear archivo INFO =====
cat > "dist/Sistema Educativo/INFO.txt" << 'EOF'
SISTEMA EDUCATIVO REP - Versión Standalone 1.0.0
=================================================

Esta es la versión standalone que incluye todo lo necesario.
NO requiere instalar MySQL ni MariaDB.

CÓMO INICIAR:

  Linux/Mac:
    1. Abrir terminal en esta carpeta
    2. Ejecutar: chmod +x iniciar.sh
    3. Ejecutar: ./iniciar.sh
    
  Alternativa Linux (con interfaz gráfica):
    1. Doble clic en "sistema-educativo.desktop"
    2. Seleccionar "Ejecutar" o "Lanzar"

REQUISITOS:
  - Java 17 o superior instalado
  - Linux (Ubuntu 18.04+, Fedora 30+, etc.) o macOS 10.14+

ARCHIVOS:
  - app.jar: Aplicación completa (~100-150 MB)
  - data/: Base de datos (se crea automáticamente)
  - logs/: Registros de la aplicación
  - backups/: Para copias de seguridad

CONSOLA H2 (Base de datos):
  URL: http://localhost:18080/h2-console
  Usuario: sa
  Contraseña: (dejar vacío)
  JDBC URL: jdbc:h2:file:./data/sistema_educativo

SERVICIOS WEB:
  - API REST: http://localhost:18080/api/
  - Swagger UI: http://localhost:18080/swagger-ui.html
  - Health Check: http://localhost:18080/actuator/health

SOLUCIÓN DE PROBLEMAS:
  1. Si no inicia, verificar que el puerto 18080 esté libre
  2. Para ver logs: tail -f logs/application.log
  3. Para reiniciar: cerrar completamente y volver a iniciar

Para más información, consulte README.txt
EOF
echo -e "   ${GREEN}✓${NC} INFO.txt creado"
echo ""

# Resumen final
echo ""
echo "=========================================="
echo -e " ${GREEN}✓ COMPLETADO EXITOSAMENTE${NC}"
echo "=========================================="
echo ""
echo "Archivos generados en: dist/Sistema Educativo/"
echo ""
echo "Contenido:"
ls -lh "dist/Sistema Educativo/" | awk '{if (NR>1) print "  " $9 " (" $5 ")"}'
echo ""

# Obtener tamaño del JAR
JAR_SIZE=$(du -h "dist/Sistema Educativo/app.jar" | cut -f1)
echo "Tamaño del archivo principal: $JAR_SIZE"
echo ""

echo -e "${YELLOW}SIGUIENTE PASO:${NC}"
echo "  Para distribuir en Linux/Mac:"
echo "    tar -czf sistema-educativo-standalone.tar.gz 'dist/Sistema Educativo/'"
echo ""
echo "  Para distribuir en cualquier plataforma:"
echo "    zip -r sistema-educativo-standalone.zip 'dist/Sistema Educativo/'"
echo ""
echo "  Los usuarios solo necesitan:"
echo "    1. Extraer el archivo"
echo "    2. Ejecutar ./iniciar.sh (Linux/Mac)"
echo ""
echo -e "${GREEN}¡Empaquetado completado!${NC}"
echo ""
