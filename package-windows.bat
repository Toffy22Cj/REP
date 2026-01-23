@echo off
REM ==========================================
REM  EMPAQUETADOR WINDOWS - SISTEMA EDUCATIVO
REM  Versión: 1.1.0 - Thin JAR + Libs
REM ==========================================
echo.
echo ==========================================
echo  EMPAQUETADOR WINDOWS - SISTEMA EDUCATIVO
echo ==========================================
echo.

REM Colores en Windows
set "GREEN=[92m"
set "RED=[91m"
set "NC=[0m"

echo [1/6] Verificando requisitos...
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo %RED%ERROR: Maven no esta instalado o no esta en PATH%NC%
    pause
    exit /b 1
)
echo    %GREEN%OK%NC% Maven encontrado
echo.

echo [2/6] Limpiando compilaciones anteriores...
if exist target rmdir /s /q target 2>nul
if exist dist rmdir /s /q dist 2>nul
mkdir dist 2>nul
echo    %GREEN%OK%NC% Carpetas limpiadas
echo.

echo [3/6] Compilando proyecto con Maven...
echo    Esto puede tomar varios minutos...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo %RED%ERROR: La compilacion fallo%NC%
    pause
    exit /b 1
)
echo    %GREEN%OK%NC% Compilacion exitosa
echo.

echo [4/6] Creando estructura de carpetas...
set "DIST_DIR=dist\Sistema Educativo REP"
mkdir "%DIST_DIR%" 2>nul
mkdir "%DIST_DIR%\data" 2>nul
mkdir "%DIST_DIR%\logs" 2>nul
mkdir "%DIST_DIR%\backups" 2>nul
echo    %GREEN%OK%NC% Estructura creada en: %DIST_DIR%
echo.

echo [5/6] Copiando archivos...

REM 1. Copiar Carpeta LIB (Dependencias)
if exist "target\lib" (
    xcopy /E /I /Y "target\lib" "%DIST_DIR%\lib" >nul
    echo    %GREEN%OK%NC% Librerias copiadas (lib\)
) else (
    echo %RED%ERROR: No se encontro target\lib%NC%
    pause
    exit /b 1
)

REM 2. Copiar JAR PRINCIPAL (Thin JAR)
if exist "target\app.jar" (
    copy /Y "target\app.jar" "%DIST_DIR%\app.jar" >nul
    echo    %GREEN%OK%NC% JAR copiado: app.jar
) else (
    echo %RED%ERROR: No se encontro target\app.jar%NC%
    echo Verifique que el build de Maven genero el archivo app.jar.
    pause
    exit /b 1
)

REM 3. Copiar AutoUpdater
if exist "target\AutoUpdater-updater.jar" (
    copy /Y "target\AutoUpdater-updater.jar" "%DIST_DIR%\AutoUpdater.jar" >nul
    echo    %GREEN%OK%NC% AutoUpdater copiado
) else (
    if exist "target\AutoUpdater.jar" (
        copy /Y "target\AutoUpdater.jar" "%DIST_DIR%\AutoUpdater.jar" >nul
        echo    %GREEN%OK%NC% AutoUpdater copiado
    )
)

REM 4. Recursos opcionales
if exist "README.md" copy /Y "README.md" "%DIST_DIR%\" >nul
if exist "LICENSE" copy /Y "LICENSE" "%DIST_DIR%\" >nul
if exist "icon.ico" copy /Y "icon.ico" "%DIST_DIR%\" >nul

echo    %GREEN%OK%NC% Archivos copiados
echo.

echo [6/6] Creando scripts de ejecucion...

REM ===== Script BAT para ejecutar =====
(
echo @echo off
echo cls
echo echo =========================================
echo echo   SISTEMA EDUCATIVO REP - STANDALONE
echo echo =========================================
echo echo.
echo echo Verificando Java...
echo java -version ^>nul 2^>^&1
echo if %%ERRORLEVEL%% NEQ 0 ^(
echo     echo ERROR: Java no esta instalado.
echo     echo.
echo     echo Por favor, instale Java 17 o superior.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo Iniciando aplicacion...
echo if not exist data mkdir data
echo if not exist logs mkdir logs
echo.
echo java -jar "app.jar"
echo.
echo if %%ERRORLEVEL%% NEQ 0 ^(
echo     echo La aplicacion cerro con error.
echo     echo Revise logs/application.log
echo     pause
echo ^)
) > "%DIST_DIR%\Iniciar.bat"
echo    %GREEN%OK%NC% Iniciar.bat creado

REM ===== Script PowerShell =====
(
echo Write-Host "SISTEMA EDUCATIVO REP" -ForegroundColor Cyan
echo Write-Host "Iniciando..." -ForegroundColor Green
echo.
echo Start-Process -FilePath "java" -ArgumentList "-jar", "app.jar" -NoNewWindow -Wait
) > "%DIST_DIR%\Iniciar-Console.ps1"

echo.
echo ==========================================
echo  %GREEN%COMPLETADO EXITOSAMENTE%NC%
echo ==========================================
echo.
echo Archivos en: %DIST_DIR%
echo.
pause >nul
explorer "%DIST_DIR%"
