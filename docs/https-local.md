# HTTPS local con certificado autofirmado

LibroLog puede arrancar en `https://localhost:8443` con un certificado PKCS12
autofirmado. Es el perfil `dev`, pensado solo para desarrollo y pruebas: no
sustituye a un certificado real en producción.

## Qué hay ya preparado

| Pieza | Ruta | Notas |
|---|---|---|
| Certificado | `backend/keystore-dev.p12` | Alias `librolog`, `CN=localhost`, SAN `localhost` y `127.0.0.1`, válido 10 años |
| Generador | `scripts/generar-keystore-dev.ps1` | Se ejecuta una sola vez; no sobreescribe |
| Confianza en Windows | `scripts/confiar-certificado-dev.ps1` | Instala el certificado para el usuario actual; no requiere administrador |
| Perfil | `backend/src/main/resources/application-dev.yml` | Puerto 8443, SSL, CORS y `require-https` |
| Contraseña | `.env` → `APP_SSL_PASSWORD` | Por defecto `changeit` |

El keystore está en `.gitignore` (`*.p12`): no se sube al repositorio.

## Arrancar desde IntelliJ IDEA

1. Abre `LibroLog/backend/pom.xml` como proyecto Maven y usa **JDK 21**
   (*File → Project Structure → Project SDK*).
2. Ejecuta `com.librolog.LibroLogApplication` una vez para que IntelliJ cree la
   configuración.
3. **Run → Edit Configurations… → LibroLogApplication**:
   - **Active profiles**: `dev`
   - **Working directory**: la carpeta `backend` (así encuentra
     `keystore-dev.p12`; es el valor por defecto de un módulo Maven)
   - *Environment variables*: no hace falta nada; `.env` se carga solo desde la
     raíz del repositorio.
4. Acepta y pulsa **Run**. En el log debe aparecer:
   ```text
   The following 1 profile is active: "dev"
   Tomcat started on port 8443 (https)
   ```
5. Abre <https://localhost:8443/>.

Si prefieres no tocar la configuración, usa el parámetro de programa
`--spring.profiles.active=dev` o la VM option
`-Dspring.profiles.active=dev`.

### Si IntelliJ sigue arrancando en 8080

Mira la primera línea del log:

```text
The following 1 profile is active: "dev"
Tomcat started on port 8443 (https)
```

Si en su lugar aparece `Tomcat started on port 8080`, IntelliJ ejecutó otra
configuración. Causas habituales:

1. Se lanzó `librolog (http)` (tipo *Application*) en lugar de `librolog`
   (tipo *Spring Boot*). Elige **librolog** en el desplegable de configuraciones.
2. Está activa la delegación de ejecución a Maven
   (*Settings → Build, Execution, Deployment → Build Tools → Maven → Runner →
   Delegate IDE build/run actions to Maven*). En ese modo la app se lanza con
   `exec:exec` y el campo **Active profiles** no se aplica. Soluciones:
   - Desmarca esa opción, o
   - añade en la configuración que uses la variable de entorno
     `SPRING_PROFILES_ACTIVE=dev`, o
   - usa `scripts\arrancar-https-dev.cmd`.
3. Quedó vivo un arranque anterior en 8080. Detenlo antes de probar.

Para comprobar qué está escuchando:

```powershell
Get-NetTCPConnection -LocalPort 8080,8443 -State Listen -ErrorAction SilentlyContinue |
    Select-Object LocalPort,OwningProcess
```

## Arrancar desde la terminal

```powershell
cd C:\ruta\a\LibroLog\backend
$env:JAVA_HOME = "C:\ruta\a\jdk-21"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

O, sin escribir nada, doble clic en `scripts\arrancar-https-dev.cmd`.

El perfil `dev` escucha **solo** en 8443. El arranque normal (sin perfil)
sigue siendo `http://localhost:8080`.

## Quitar el aviso del navegador

Con el certificado autofirmado, la primera visita muestra
«La conexión no es privada». Puedes:

- Pulsar **Avanzado → Continuar a localhost (no seguro)** cada vez que reinicies
  el navegador, o
- Ejecutar una sola vez:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts\confiar-certificado-dev.ps1
  ```
  Windows mostrará una advertencia de seguridad; pulsa **Sí**. El certificado
  queda en *Entidades de certificación raíz de confianza* del usuario actual.
  Cierra y vuelve a abrir el navegador.

  Para deshacerlo: abre `certlm.msc` → *Entidades de certificación raíz de
  confianza → Certificados*, busca `localhost` con huella
  `18664B6D227E88F673BB71357EF8BB73E532DB56` y bórralo. También puedes usar
  `certmgr.msc` (almacén del usuario).

Firefox no usa el almacén de Windows: si muestra el aviso, pulsa
**Avanzado → Aceptar el riesgo y continuar**.

## Comprobar que funciona

```powershell
curl.exe -k -s -o NUL -w "health=%{http_code}\n" https://localhost:8443/actuator/health
curl.exe -k -s https://localhost:8443/api/auth/providers
```

Debe responder `200` y `{"local":true,"google":true}` si Google está
configurado.

Cabeceras esperadas en `dev`:

- `Content-Security-Policy`, `X-Content-Type-Options`, `Referrer-Policy`,
  `X-Frame-Options: DENY` y `Permissions-Policy`.
- `Set-Cookie: librolog_refresh=…; Secure; HttpOnly; SameSite=Strict`.
- **Sin** `Strict-Transport-Security`: se desactiva en `dev` para no fijar
  `localhost` a HTTPS en el navegador durante un año (rompería
  `http://localhost:8080` y otros servicios locales). En producción
  `app.security.hsts-enabled` vale `true` por defecto.

## Google OAuth2 con HTTPS

Google exige que el `redirect_uri` coincida exactamente con el que usa la
aplicación. En **Google Cloud Console → Credenciales → tu cliente OAuth →
URI de redirección autorizados** deben estar los dos (o solo el que uses):

```text
http://localhost:8080/login/oauth2/code/google
https://localhost:8443/login/oauth2/code/google
```

Y en **Orígenes de JavaScript autorizados**:

```text
http://localhost:8080
https://localhost:8443
```

Si falta el de 8443, Google devuelve `redirect_uri_mismatch`.

## Regenerar el certificado

```powershell
Remove-Item backend\keystore-dev.p12
powershell -ExecutionPolicy Bypass -File scripts\generar-keystore-dev.ps1 -Password changeit
powershell -ExecutionPolicy Bypass -File scripts\confiar-certificado-dev.ps1 -Password changeit
```

Si cambias la contraseña, actualiza también `APP_SSL_PASSWORD` en `.env`.

## Errores frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `Port 8443 was already in use` | Quedó otro arranque vivo | Cierra el proceso o cambia `server.port` en el perfil |
| `Tomcat started on port 8080` con perfil `dev` | IntelliJ arrancó otra configuración (o delegó en Maven) y no aplicó el perfil | Usa la configuración Spring Boot `librolog`, añade la variable `SPRING_PROFILES_ACTIVE=dev` o ejecuta `scripts\arrancar-https-dev.cmd` |
| `ERR_CONNECTION_REFUSED` en 8443 | No hay nada escuchando en 8443: la app está en 8080 | Revisa el puerto en el log (`Tomcat started on port …`) |
| `Port 8443 was already in use` tras cerrar la app | Proceso Java huérfano | `Get-NetTCPConnection -LocalPort 8443` y detén el PID |
| `keystore password was incorrect` | `APP_SSL_PASSWORD` no coincide | Corrige `.env` o regenera con la misma contraseña |
| `DerInputStream.getLength(): lengthTag=…` | El archivo no es PKCS12 o está corrupto | Regenera el keystore |
| El navegador sigue avisando | Certificado no confiado o Firefox | Ejecuta el script de confianza o acepta el riesgo |
| `redirect_uri_mismatch` al entrar con Google | Falta el URI de 8443 en Google Console | Añádelo y espera unos minutos |
| `403` en CORS desde `http://localhost:8080` | El perfil `dev` solo permite 8443 | Abre la app en `https://localhost:8443` |

## Producción

El perfil `dev` no se usa en producción: allí el TLS lo termina el proxy
inverso (Nginx, Caddy, balanceador) y la aplicación puede seguir en HTTP detrás
de él, con `APP_REQUIRE_HTTPS=true` y `app.security.hsts-enabled=true`.
