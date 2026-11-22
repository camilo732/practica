# ---------------------------------------------------------------------------
# Genera un certificado PKCS12 autofirmado para el perfil "dev" de LibroLog.
#
#   powershell -ExecutionPolicy Bypass -File scripts\generar-keystore-dev.ps1
#
# Crea backend\keystore-dev.p12 con validez de 10 años para localhost.
# El archivo está en .gitignore: es un certificado local, no de producción.
# ---------------------------------------------------------------------------
param(
    [string]$Password = "changeit"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $root "backend\keystore-dev.p12"

if (Test-Path $keystore) {
    Write-Host "El keystore ya existe: $keystore"
    Write-Host "Bórralo si quieres regenerarlo."
    exit 0
}

if ($env:JAVA_HOME) {
    $keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
} else {
    $keytool = "keytool"
}

& $keytool -genkeypair `
    -alias librolog `
    -keyalg RSA `
    -keysize 2048 `
    -storetype PKCS12 `
    -keystore $keystore `
    -validity 3650 `
    -storepass $Password `
    -keypass $Password `
    -dname "CN=localhost, OU=LibroLog, O=LibroLog, C=CO" `
    -ext "SAN=dns:localhost,ip:127.0.0.1"

Write-Host ""
Write-Host "Certificado creado en: $keystore"
Write-Host "Contraseña del keystore: $Password (valor por defecto de application-dev.yml)"
