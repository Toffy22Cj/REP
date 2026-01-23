#!/bin/bash
# ==========================================
# EMPAQUETADOR UNIVERSAL (Bash) - SISTEMA EDUCATIVO
# Versión: 1.1.0 - Robust & Clean
# ==========================================

# Definir colores de forma compatible
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo ""
echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN} EMPAQUETADOR LINUX/MAC - SISTEMA EDUCATIVO${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""

# [1/6] Verificar requisitos
echo -e "${CYAN}[1/6]${NC} Verificando requisitos..."

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven no está instalado${NC}"
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Maven encontrado: $(mvn -version | head -n 1)"

if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java no está instalado${NC}"
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Java encontrado: $(java -version | head -n 1)"
echo ""

# [2/6] Limpiar compilaciones anteriores
echo -e "${CYAN}[2/6]${NC} Limpiando compilaciones anteriores..."
rm -rf target dist
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
    exit 1
fi
echo -e "   ${GREEN}✓${NC} Compilación exitosa"
echo ""

# [4/6] Crear estructura de carpetas
echo -e "${CYAN}[4/6]${NC} Creando estructura de carpetas..."
DIST_DIR="dist/Sistema Educativo REP"
mkdir -p "$DIST_DIR"
mkdir -p "$DIST_DIR/data"
mkdir -p "$DIST_DIR/logs"
mkdir -p "$DIST_DIR/backups"
echo -e "   ${GREEN}✓${NC} Estructura creada en: $DIST_DIR"
echo ""

# [5/6] Copiar archivos
echo -e "${CYAN}[5/6]${NC} Copiando archivos..."

# Copiar carpeta LIB (Dependencias)
if [ -d "target/lib" ]; then
    cp -r "target/lib" "$DIST_DIR/lib"
    echo -e "   ${GREEN}✓${NC} Librerías copiadas (target/lib -> dist/.../lib)"
else
    echo -e "${RED}ERROR: No se encontró target/lib${NC}"
    exit 1
fi

# Copiar JAR PRINCIPAL (Thin JAR)
MAIN_JAR="target/app.jar"

if [ -f "$MAIN_JAR" ]; then
    cp "$MAIN_JAR" "$DIST_DIR/app.jar"
    echo -e "   ${GREEN}✓${NC} JAR copiado: app.jar"
else
    echo -e "${RED}ERROR: No se encontró $MAIN_JAR${NC}"
    echo "Contenido de target/:"
    ls -la target/
    exit 1
fi

# Copiar AutoUpdater
if [ -f "target/AutoUpdater-updater.jar" ]; then
    cp "target/AutoUpdater-updater.jar" "$DIST_DIR/AutoUpdater.jar"
    echo -e "   ${GREEN}✓${NC} AutoUpdater copiado"
elif [ -f "target/AutoUpdater.jar" ]; then
    cp "target/AutoUpdater.jar" "$DIST_DIR/AutoUpdater.jar"
    echo -e "   ${GREEN}✓${NC} AutoUpdater copiado"
fi

# Copiar recursos estáticos opcionales
[ -f "README.md" ] && cp "README.md" "$DIST_DIR/"
[ -f "LICENSE" ] && cp "LICENSE" "$DIST_DIR/"
[ -f "icon.ico" ] && cp "icon.ico" "$DIST_DIR/"

echo -e "   ${GREEN}✓${NC} Archivos copiados"
echo ""

# [6/6] Crear scripts de ejecución
echo -e "${CYAN}[6/6]${NC} Creando lanzadores..."

# --- Script inicia.sh (Universal Linux/Mac) ---
cat > "$DIST_DIR/iniciar.sh" << 'EOF'
#!/bin/bash
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

clear
echo -e "${CYAN}=========================================${NC}"
echo -e "${CYAN}  SISTEMA EDUCATIVO REP - STANDALONE    ${NC}"
echo -e "${CYAN}=========================================${NC}"
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java no encontrado.${NC}"
    echo "Instale Java 17+ y asegúrese de que esté en el PATH."
    read -p "Presione Enter..."
    exit 1
fi

# Verificar versión (simple)
JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VER" -lt 17 ] && [ "$JAVA_VER" -ne 1 ]; then # handle 1.8 as 1
     echo -e "${RED}Advertencia: Se detectó Java $JAVA_VER. Se recomienda Java 17+.${NC}"
fi

echo -e "${GREEN}Iniciando aplicación...${NC}"
mkdir -p data logs backups

# Ejecutar sin depender de application.properties externo
java -jar app.jar

EXIT_CODE=$?
echo ""
if [ $EXIT_CODE -ne 0 ]; then
    echo -e "${RED}La aplicación se cerró con error ($EXIT_CODE).${NC}"
    echo "Revise logs/application.log"
else
    echo -e "${GREEN}Aplicación finalizada.${NC}"
fi
read -p "Presione Enter para cerrar..."
EOF

chmod +x "$DIST_DIR/iniciar.sh"
echo -e "   ${GREEN}✓${NC} Script iniciar.sh creado"

# --- Archivo .desktop (Linux) ---
if [[ "$(uname)" == "Linux" ]]; then
    cat > "$DIST_DIR/sistema-educativo.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=Sistema Educativo REP
Comment=Sistema de gestión educativa
Exec=bash -c 'cd "$(dirname "%k")" && ./iniciar.sh'
Icon=applications-education
Terminal=true
Categories=Education;
EOF
    chmod +x "$DIST_DIR/sistema-educativo.desktop"
    echo -e "   ${GREEN}✓${NC} Launcher .desktop creado"
fi

echo ""
echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}  ¡PROCESO TERMINADO!  ${NC}"
echo -e "${CYAN}==========================================${NC}"
echo -e "Su aplicación portable está en:"
echo -e "${YELLOW}  $DIST_DIR${NC}"
echo ""
echo "Para probarla ahora:"
echo "  cd \"$DIST_DIR\""
echo "  ./iniciar.sh"
echo ""
