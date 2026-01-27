#!/bin/bash

# GUÍA RÁPIDA DE REFERENCIA

# 26 de Enero de 2026

# ============ COMANDOS ESENCIALES ============

# 1. CARGAR VARIABLES DE ENTORNO

source .env.local

# 2. COMPILAR PROYECTO

mvn clean package -DskipTests

# 3. EJECUTAR APLICACIÓN

# Opción A: Desarrollo (con logs detallados)

java -jar target/main-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Opción B: Producción (logs limitados, seguro)

java -Dspring.profiles.active=prod -jar target/main-0.0.1-SNAPSHOT.jar

# Opción C: Con puerto diferente

java -jar target/main-0.0.1-SNAPSHOT.jar -Dserver.port=8081 --spring.profiles.active=dev

# 4. VERIFICAR SEGURIDAD

bash check-security.sh

# 5. VER VARIABLES CONFIGURADAS

./setup-env.sh

# ============ CREDENCIALES ============

# Base de datos: localhost:3306/colegio

# Usuario: admin

# Contraseña: admin

# JWT_SECRET: guxs6E+roAhbydKp6hFYVpwoJQbVNV9cOtV6X7VPA9JVG4hKhwwuubMr3ddPJ9kKQqjXu4YplHyKoVhN3u2Dfg==

# ============ ENDPOINTS PRINCIPALES ============

# Registro de usuarios

curl -X POST http://localhost:8080/api/registro \
 -H "Content-Type: application/json" \
 -d '{"identificacion":"123","nombre":"Test","email":"test@example.com","password":"123456","rol":"ESTUDIANTE"}'

# Login

curl -X POST http://localhost:8080/api/auth/login \
 -H "Content-Type: application/json" \
 -d '{"identificacion":"admin","password":"admin"}'

# Obtener token (después de login)

TOKEN="tu_token_aqui"

# Acceder a endpoint protegido

curl -X GET http://localhost:8080/api/admin/ \
 -H "Authorization: Bearer $TOKEN"

# ============ ARCHIVOS IMPORTANTES ============

# .env.local - Variables de entorno (NO commitear)

# application.properties - Configuración desarrollo

# application-prod.properties - Configuración producción

# setup-env.sh - Script de setup

# check-security.sh - Verificación de seguridad

# CONFIGURACION_COMPLETADA.md - Documentación

# ============ TROUBLESHOOTING ============

# Error: Variables de entorno no encontradas

# Solución: source .env.local

# Error: Base de datos no conecta

# Solución: Verificar que MySQL está corriendo

# mysql -u admin -p -h localhost

# use colegio;

# show tables;

# Error: JWT Secret inválido

# Solución: Regenerar con: openssl rand -base64 64

# Actualizar en .env.local

# Error: CORS bloqueado

# Solución: Actualizar ALLOWED_ORIGINS en .env.local

# Reiniciar aplicación

# ============ DEPLOY A PRODUCCIÓN ============

# 1. Crear variables en servidor

export DB_HOST="db.producción.com"
export DB_PORT="3306"
export DB_NAME="colegio_prod"
export DB_USERNAME="prod_user"
export DB_PASSWORD="contraseña_fuerte"
export JWT_SECRET="<256_bits_en_base64>"

# 2. Obtener certificado SSL

certbot certonly --standalone -d midominio.com

# 3. Crear keystore PKCS12

keytool -genkey -alias tomcat -storetype PKCS12 \
 -keyalg RSA -keysize 2048 -keystore keystore.p12 \
 -validity 365 -storepass changeit

# 4. Ejecutar con SSL

java -Dspring.profiles.active=prod \
 -Dserver.ssl.enabled=true \
 -Dserver.ssl.key-store=/path/to/keystore.p12 \
 -Dserver.ssl.key-store-password=changeit \
 -jar target/main-0.0.1-SNAPSHOT.jar

# ============ MONITOREO ============

# Ver logs en tiempo real

tail -f logs/mi-aplicacion.log

# Buscar errores en logs

grep "ERROR" logs/mi-aplicacion.log

# Ver solicitudes HTTP

grep "GET\|POST\|PUT\|DELETE" logs/mi-aplicacion.log

# ============ CLEAN UP ============

# Limpiar archivos temporales

mvn clean

# Resetear base de datos (cuidado!)

# mysql -u admin -p colegio < schema.sql

# ============ INFORMACIÓN DE SEGURIDAD ============

# Score actual: 80/100

# Vulnerabilidades críticas: 0

# Variables de entorno: Configuradas ✓

# JWT Secret: Externalizado ✓

# CORS: Configurado ✓

# Rate Limiting: Habilitado ✓

# SSL/TLS: Falta (para producción)

# ============ PRÓXIMAS TAREAS ============

# [ ] Testear con spring.profiles.active=prod

# [ ] Obtener certificado SSL

# [ ] Ejecutar dependency-check

# [ ] Code review de seguridad

# [ ] Deploy a staging

# [ ] Testing de carga

# [ ] Deploy a producción

# ============ LINKS ÚTILES ============

# Spring Boot: https://spring.io/projects/spring-boot

# Spring Security: https://spring.io/projects/spring-security

# JWT: https://tools.ietf.org/html/rfc8725

# OWASP Top 10: https://owasp.org/www-project-top-ten/

# MySQL Connector: https://dev.mysql.com/doc/connector-j/

# ============ CONTACTO Y SOPORTE ============

# Documentación: CONFIGURACION_COMPLETADA.md

# Análisis Detallado: ESTADO_ACTUAL.md

# Plan de Implementación: PLAN_IMPLEMENTACION.md

# Recomendaciones: RECOMENDACIONES_SEGURIDAD.md
