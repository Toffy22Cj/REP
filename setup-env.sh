#!/bin/bash

# Script para cargar variables de entorno y ejecutar la aplicación
# Uso: source setup-env.sh

echo "🔐 Cargando variables de entorno..."

# Verificar si existe .env.local
if [ -f .env.local ]; then
    source .env.local
    echo "✓ Variables desde .env.local"
else
    echo "❌ ERROR: No encontré .env.local"
    echo "   Por favor crea el archivo .env.local con las variables de entorno"
    exit 1
fi

# Verificar que las variables críticas estén configuradas
REQUIRED_VARS=("DB_USERNAME" "DB_PASSWORD" "JWT_SECRET")
MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
    fi
done

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo "❌ ERROR: Variables faltantes: ${MISSING_VARS[*]}"
    exit 1
fi

echo "✓ Base de datos: $DB_HOST:$DB_PORT/$DB_NAME"
echo "✓ Usuario BD: $DB_USERNAME"
echo "✓ JWT Secret: ${JWT_SECRET:0:20}..."
echo "✓ Puerto: $SERVER_PORT"
echo "✓ CORS Origins: $ALLOWED_ORIGINS"

echo ""
echo "✅ Ambiente configurado correctamente"
echo ""
echo "Para iniciar la aplicación en DESARROLLO:"
echo "  java -jar target/main-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
echo ""
echo "Para iniciar en PRODUCCIÓN:"
echo "  java -Dspring.profiles.active=prod -jar target/main-0.0.1-SNAPSHOT.jar"
