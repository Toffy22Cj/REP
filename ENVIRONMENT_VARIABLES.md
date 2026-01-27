#!/bin/bash

# Guía de Configuración de Variables de Entorno

## ========== DATABASE VARIABLES ==========

# DB_HOST: Dirección IP o hostname del servidor MySQL

# - Local: localhost

# - Docker: mysql-service

# - Cloud: db-instance.c9akciq32.us-east-1.rds.amazonaws.com

# Requerido: SÍ

# Ejemplo: DB_HOST="db.ejemplo.com"

# DB_PORT: Puerto de MySQL (default: 3306)

# Requerido: NO (default: 3306)

# DB_NAME: Nombre de la base de datos

# Requerido: SÍ

# Ejemplo: DB_NAME="colegio_prod"

# DB_USERNAME: Usuario de acceso a la BD

# CRÍTICO: Usar usuario con permisos limitados

# NO: Usar root

# Ejemplo: DB_USERNAME="colegio_app"

# DB_PASSWORD: Contraseña del usuario

# CRÍTICO: Generar contraseña fuerte de 16+ caracteres

# Generar con: openssl rand -base64 32 | tr -d "=+/" | cut -c1-25

# Ejemplo: DB_PASSWORD="a8Kxj9Lm2QwEr3Ty5Ui7Op0As"

## ========== JWT VARIABLES ==========

# JWT_SECRET: Clave para firmar tokens JWT

# CRÍTICO: Generar con al menos 256 bits (32 bytes)

# Generar con: openssl rand -base64 64

# - NUNCA: Usar contraseña simple

# - NUNCA: Usar misma clave en todos los ambientes

# - NUNCA: Compartir en público

# Cambiar: Cada 6-12 meses

# Ejemplo: JWT_SECRET="W7wKmZXkN9L2cQ5tP8vJ3kL4mN6qR9sT..."

## ========== SSL/TLS VARIABLES ==========

# SSL_KEYSTORE_PATH: Ruta del archivo keystore (formato PKCS12)

# Generar con:

# keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \

# -storetype PKCS12 -keystore tomcat.p12 -validity 365

# Requerido: SÍ (para HTTPS)

# Ejemplo: SSL_KEYSTORE_PATH="/etc/ssl/keystore/tomcat.p12"

# SSL_KEYSTORE_PASSWORD: Contraseña del keystore

# Requerido: SÍ

# Ejemplo: SSL_KEYSTORE_PASSWORD="KeystorePass123!@#"

# SSL_KEY_ALIAS: Alias de la clave en el keystore

# Default: tomcat

# Requerido: NO

## ========== SERVER VARIABLES ==========

# SERVER_PORT: Puerto en el que corre la aplicación

# Default: 8443 (HTTPS)

# Requerido: NO

## ========== LOGGING VARIABLES ==========

# LOG_PATH: Directorio donde guardar logs

# Requerido: NO (default: ./logs)

# Ejemplo: LOG_PATH="/var/log/rep"

## ========== SECURITY VARIABLES ==========

# ALLOWED_ORIGINS: Orígenes permitidos para CORS (separados por comas)

# CRÍTICO: NO usar \*

# Ejemplo: ALLOWED_ORIGINS="https://app.ejemplo.com,https://admin.ejemplo.com"

# SESSION_TIMEOUT: Timeout de sesión en segundos

# Recomendado: 1800 (30 minutos)

# Ejemplo: SESSION_TIMEOUT="1800"

## ========== EMAIL CONFIGURATION ==========

# MAIL_HOST: Servidor SMTP

# Ejemplo: MAIL_HOST="smtp.gmail.com"

# MAIL_PORT: Puerto SMTP

# Gmail: 587 (TLS)

# Otros: Verificar

# MAIL_USERNAME: Usuario de email para notificaciones

# CRÍTICO: No exponer credenciales

# MAIL_PASSWORD: Contraseña o App Password

# Para Gmail: Usar App Password, NO contraseña de cuenta

## ========== MONITORING & BACKUP ==========

# PROMETHEUS_ENABLED: Habilitar métricas Prometheus

# BACKUP_ENABLED: Habilitar backup automático

# BACKUP_SCHEDULE: Cron para backup automático

## ========== DEPLOYMENT INSTRUCTIONS ==========

# 1. Crear archivo .env seguro (NO en Git)

# touch .env && chmod 600 .env

#

# 2. Generar valores seguros:

# - JWT_SECRET: openssl rand -base64 64

# - DB_PASSWORD: openssl rand -base64 32 | tr -d "=+/" | cut -c1-25

# - SSL_KEYSTORE_PASSWORD: openssl rand -base64 16

#

# 3. Editar .env con valores reales:

# nano .env

#

# 4. Cargar variables y ejecutar:

# source .env

# java -jar application.jar --spring.profiles.active=prod

#

# 5. Verificar ejecución:

# curl -k https://localhost:8443/api/health

#

# 6. Monitorar logs:

# tail -f /var/log/rep/application.log | grep ERROR

## ========== SECURITY CHECKLIST ==========

# ☐ .env está en .gitignore

# ☐ .env tiene permisos 600 (solo lectura para owner)

# ☐ Passwords generados con al menos 16 caracteres

# ☐ JWT_SECRET tiene 32+ bytes

# ☐ SSL certificate es válido y no autoasignado

# ☐ DB_USERNAME NO es 'root'

# ☐ DB tiene backup automático

# ☐ Logs se escriben en carpeta segura

# ☐ Variables NO están en código fuente

# ☐ Acceso a servidor restringido por firewall

# ☐ Monitoreo y alertas configuradas

# ☐ Plan de incidentes documentado

## ========== TROUBLESHOOTING ==========

# Error: "Cannot create PoolableConnectionFactory"

# - Verificar credenciales de BD

# - Verificar conectividad a servidor DB

# - Verificar firewall/security groups

# Error: "JWT signature does not match"

# - Verificar JWT_SECRET es igual en todas instancias

# - Verificar no hubo cambio de clave sin actualizar tokens

# Error: "SSL certificate is not trusted"

# - Verificar path y contraseña de keystore

# - Verificar alias existe en keystore

# - Generar nuevo con: keytool -list -v -keystore tomcat.p12

# Error: "Permission denied" en logs

# - Verificar permisos en LOG_PATH

# - chmod 755 /var/log/rep

## ========== REFERENCES ==========

# OWASP: https://owasp.org/www-community/attacks/injection

# Spring Security: https://spring.io/projects/spring-security

# JWT Best Practices: https://tools.ietf.org/html/rfc8725

# MySQL SSL: https://dev.mysql.com/doc/
