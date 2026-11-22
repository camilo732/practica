# ---------------------------------------------------------------------------
# Confía en el certificado autofirmado del perfil "dev" para el usuario actual.
#
#   powershell -ExecutionPolicy Bypass -File scripts\confiar-certificado-dev.ps1
#
# No requiere administrador: instala el certificado en "Entidades de
# certificación raíz de confianza" del usuario actual. Con esto Chrome, Edge y
# el resto de navegadores que usan el almacén de Windows dejan de mostrar el
# aviso al abrir https://localhost:8443.
#
# Windows mostrará una advertencia de seguridad una sola vez; hay que pulsar
# "Sí" para confirmar. Para deshacerlo, ver docs/https-local.md.
# ---------------------------------------------------------------------------
param(
    [string]$Password = "changeit",
    [string]$Keystore = (Join-Path (Split-Path -Parent $PSScriptRoot) "backend\keystore-dev.p12")
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Keystore)) {
    Write-Host "No existe el keystore: $Keystore"
    Write-Host "Genéralo primero con scripts\generar-keystore-dev.ps1."
    exit 1
}

$collection = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2Collection
$collection.Import(
    (Resolve-Path $Keystore).Path,
    $Password,
    [System.Security.Cryptography.X509Certificates.X509KeyStorageFlags]::EphemeralKeySet)

$certificate = $collection | Where-Object { $_.HasPrivateKey } | Select-Object -First 1
if (-not $certificate) {
    Write-Host "El keystore no contiene ninguna entrada con clave privada."
    exit 1
}

$store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root", "CurrentUser")
$store.Open("ReadWrite")

try {
    $installed = $store.Certificates | Where-Object { $_.Thumbprint -eq $certificate.Thumbprint }
    if ($installed) {
        Write-Host "El certificado ya estaba confiado para este usuario."
    } else {
        $store.Add($certificate)
        Write-Host "Certificado instalado en Entidades de certificación raíz del usuario actual."
    }
} finally {
    $store.Close()
}

Write-Host ""
Write-Host "Huella SHA-1:  $($certificate.Thumbprint)"
Write-Host "Válido hasta:  $($certificate.NotAfter)"
Write-Host "Cierra y vuelve a abrir el navegador: https://localhost:8443 ya no avisará."
Write-Host "Firefox usa su propio almacén: si muestra el aviso, pulsa 'Avanzado > Aceptar el riesgo'."
