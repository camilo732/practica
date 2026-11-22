@echo off
rem ---------------------------------------------------------------------------
rem Arranca LibroLog con HTTPS local en https://localhost:8443
rem Equivale a: mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
rem
rem Uso: doble clic o, desde una terminal:
rem   scripts\arrancar-https-dev.cmd
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0..\backend" || exit /b 1

rem JAVA_HOME: si no esta definido, se busca un JDK 21 descargado por IntelliJ
rem en %USERPROFILE%\.jdks (sin rutas personales escritas en el script).
if not defined JAVA_HOME for /d %%D in ("%USERPROFILE%\.jdks\*21*") do set "JAVA_HOME=%%D"
if not defined JAVA_HOME echo Aviso: JAVA_HOME no esta definido; se usara el java del PATH.
if defined JAVA_HOME echo Usando JAVA_HOME=%JAVA_HOME%

rem El certificado local no se sube a GitHub: hay que generarlo una vez.
if not exist "keystore-dev.p12" (
    echo Falta backend\keystore-dev.p12. Generalo una vez con:
    echo   powershell -ExecutionPolicy Bypass -File ..\scripts\generar-keystore-dev.ps1
    pause
    exit /b 1
)

rem Sin .env no hay contrasena de base de datos ni JWT_SECRET.
if not exist "..\.env" (
    echo Falta el archivo .env en la raiz. Copia .env.example a .env y rellenalo.
    pause
    exit /b 1
)

rem La forma más fiable de activar el perfil, también si IntelliJ lanza la app
rem a través de Maven: una variable de entorno que hereda el proceso Java.
set "SPRING_PROFILES_ACTIVE=dev"

echo Arrancando LibroLog con el perfil "dev"...
echo URL: https://localhost:8443/
echo Para detenerlo: Ctrl+C
echo.

call mvnw.cmd spring-boot:run
if errorlevel 1 pause
endlocal
