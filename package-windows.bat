@echo off
REM ==========================================
REM  EMPAQUETADOR WINDOWS - SISTEMA EDUCATIVO
REM  Versión: 1.0.0
REM ==========================================
echo.
echo ==========================================
echo  EMPAQUETADOR WINDOWS - SISTEMA EDUCATIVO
echo ==========================================
echo.

REM Colores en Windows (opcional, comentar si causa problemas)
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "NC=[0m"

echo [1/6] Verificando requisitos...
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo %RED%ERROR: Maven no esta instalado o no esta en PATH%NC%
    echo Por favor instale Maven: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo    %GREEN%Checkmark%NC% Maven encontrado: 
mvn -version | findstr "Apache Maven"
echo.

echo [2/6] Limpiando compilaciones anteriores...
if exist target rmdir /s /q target 2>nul
if exist dist rmdir /s /q dist 2>nul
mkdir dist 2>nul
echo    %GREEN%Checkmark%NC% Carpetas limpiadas
echo.

echo [3/6] Compilando proyecto con Maven...
echo    Esto puede tomar varios minutos...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo %RED%ERROR: La compilacion fallo%NC%
    echo Revise los mensajes de error anteriores.
    pause
    exit /b 1
)
echo    %GREEN%Checkmark%NC% Compilacion exitosa
echo.

echo [4/6] Creando estructura de carpetas...
mkdir "dist\Sistema Educativo" 2>nul
mkdir "dist\Sistema Educativo\data" 2>nul
mkdir "dist\Sistema Educativo\logs" 2>nul
mkdir "dist\Sistema Educativo\backups" 2>nul
echo    %GREEN%Checkmark%NC% Estructura creada
echo.

echo [5/6] Copiando archivos...
if exist "target\sistema-educativo-standalone.jar" (
    copy /Y "target\sistema-educativo-standalone.jar" "dist\Sistema Educativo\app.jar" >nul
    echo    %GREEN%Checkmark%NC% JAR copiado: app.jar
) else (
    echo %RED%ERROR: No se encontro el JAR compilado%NC%
    echo Ubicacion esperada: target\sistema-educativo-standalone.jar
    pause
    exit /b 1
)

REM Copiar archivos opcionales si existen
if exist "README-standalone.txt" (
    copy /Y "README-standalone.txt" "dist\Sistema Educativo\README.txt" >nul
    echo    %GREEN%Checkmark%NC% README copiado
)
if exist "LICENSE.txt" (
    copy /Y "LICENSE.txt" "dist\Sistema Educativo\" >nul
    echo    %GREEN%Checkmark%NC% LICENSE copiado
)
if exist "colegio.png" (
    copy /Y "colegio.png" "dist\Sistema Educativo\" >nul
    echo    %GREEN%Checkmark%NC% Icono copiado
)
echo.

echo [6/6] Creando scripts de ejecucion...

REM ===== Script BAT para ejecutar =====
(
echo @echo off
echo cls
echo echo =========================================
echo echo   SISTEMA EDUCATIVO REP - STANDALONE
echo echo   Version 1.0.0
echo echo =========================================
echo echo.
echo echo Verificando Java...
echo java -version ^>nul 2^>^&1
echo if %%ERRORLEVEL%% NEQ 0 ^(
echo     echo ERROR: Java no esta instalado.
echo     echo.
echo     echo Por favor, instale Java 17 o superior:
echo     echo https://adoptium.net/
echo     echo.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo Java encontrado. Iniciando aplicacion...
echo echo.
echo java -jar "app.jar"
echo.
echo echo.
echo echo Aplicacion finalizada.
echo pause
) > "dist\Sistema Educativo\Iniciar.bat"
echo    %GREEN%Checkmark%NC% Iniciar.bat creado

REM ===== Script PowerShell para ejecutar =====
(
echo #!/usr/bin/env pwsh
echo # Script PowerShell para Sistema Educativo REP
echo.
echo Write-Host "=========================================" -ForegroundColor Cyan
echo Write-Host "  SISTEMA EDUCATIVO REP - STANDALONE    " -ForegroundColor Cyan
echo Write-Host "  Version 1.0.0                         " -ForegroundColor Cyan
echo Write-Host "=========================================" -ForegroundColor Cyan
echo Write-Host ""
echo.
echo # Verificar Java
echo Write-Host "Verificando Java..." -ForegroundColor Yellow
echo try {
echo     $javaVersion = java -version 2^>^&1 ^| Select-String "version"
echo     Write-Host "Java encontrado: $javaVersion" -ForegroundColor Green
echo } catch {
echo     Write-Host "ERROR: Java no esta instalado" -ForegroundColor Red
echo     Write-Host "Descargue Java desde: https://adoptium.net/" -ForegroundColor Yellow
echo     Read-Host "Presione Enter para salir"
echo     exit 1
echo }
echo.
echo Write-Host ""
echo Write-Host "Iniciando aplicacion..." -ForegroundColor Green
echo Write-Host ""
echo.
echo # Ejecutar aplicacion
echo java -jar "app.jar"
echo.
echo Write-Host ""
echo Write-Host "Aplicacion finalizada" -ForegroundColor Yellow
echo Read-Host "Presione Enter para salir"
) > "dist\Sistema Educativo\Iniciar.ps1"
echo    %GREEN%Checkmark%NC% Iniciar.ps1 creado

REM ===== Crear archivo de información =====
(
echo SISTEMA EDUCATIVO REP - Version Standalone 1.0.0
echo ================================================
echo.
echo Esta es la version standalone que incluye todo lo necesario.
echo NO requiere instalar MySQL ni MariaDB.
echo.
echo COMO INICIAR:
echo   1. Doble clic en "Iniciar.bat"
echo   2. Espere a que aparezca la ventana de login
echo   3. Listo!
echo.
echo REQUISITOS:
echo   - Java 17 o superior instalado
echo   - Windows 7 o superior
echo.
echo ARCHIVOS:
echo   - app.jar: Aplicacion completa
echo   - data/: Base de datos ^(se crea automaticamente^)
echo   - logs/: Registros de la aplicacion
echo.
echo CONSOLA H2 ^(Base de datos^):
echo   URL: http://localhost:18080/h2-console
echo   Usuario: sa
echo   Contrasena: ^(dejar vacio^)
echo.
echo Para mas informacion, consulte README.txt
) > "dist\Sistema Educativo\INFO.txt"
echo    %GREEN%Checkmark%NC% INFO.txt creado
echo.

echo.
echo ==========================================
echo  %GREEN%COMPLETADO EXITOSAMENTE%NC%
echo ==========================================
echo.
echo Archivos generados en: dist\Sistema Educativo\
echo.
echo Contenido:
dir /B "dist\Sistema Educativo"
echo.
echo SIGUIENTE PASO:
echo   1. Comprimir la carpeta "Sistema Educativo" en ZIP
echo   2. Distribuir el archivo ZIP
echo   3. Los usuarios solo extraen y ejecutan "Iniciar.bat"
echo.
echo Tamano del archivo principal:
for %%A in ("dist\Sistema Educativo\app.jar") do echo    app.jar: %%~zA bytes ^(~%%~zA:~0,-6%% MB^)
echo.
echo Presione cualquier tecla para abrir la carpeta...
pause >nul
explorer "dist\Sistema Educativo"
