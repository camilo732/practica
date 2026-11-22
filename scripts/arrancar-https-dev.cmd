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

rem JAVA_HOME debe apuntar a un JDK 21 (o tener java 21 en el PATH).
if not defined JAVA_HOME echo Aviso: JAVA_HOME no esta definido; se usara el java del PATH.

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
