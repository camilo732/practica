# Configurar el acceso con Google (Fase 7)

Tener una cuenta de Gmail **no basta**. Google exige que LibroLog se registre
como aplicación OAuth en Google Cloud y que reciba un **Client ID** y un
**Client secret**. Estos pasos se hacen una sola vez.

## 1. Crear el proyecto

1. Abrir <https://console.cloud.google.com/> con la cuenta de Google que se
   usará para entrar a LibroLog.
2. Arriba a la izquierda, abrir el selector de proyectos y pulsar
   **Nuevo proyecto**.
3. Nombre: `LibroLog` (o el que se prefiera). Pulsar **Crear** y esperar a que
   aparezca en la lista.
4. Seleccionar ese proyecto.

## 2. Configurar la pantalla de consentimiento

1. Ir a **APIs y servicios → Pantalla de consentimiento de OAuth**
   (en algunas cuentas aparece como **Google Auth Platform**).
2. Si pide configuración inicial, pulsar **Comenzar**.
3. **Información de la aplicación**
   - Nombre de la aplicación: `LibroLog`
   - Correo de asistencia: el correo de la cuenta
   - Correo del desarrollador: el mismo correo
   - Público: **Externo** (o **Interno** si es una cuenta de Google Workspace)
4. **Público / Usuarios de prueba**: añadir el correo de Gmail con el que se
   va a iniciar sesión. Mientras la aplicación esté en modo **Prueba**, solo
   esos correos pueden entrar.
5. Guardar. No hace falta publicar la aplicación para probar en local.

## 3. Crear el cliente OAuth

1. Ir a **APIs y servicios → Credenciales**.
2. Pulsar **Crear credenciales → ID de cliente de OAuth**.
3. Tipo de aplicación: **Aplicación web**.
4. Nombre: `LibroLog local`.
5. **Orígenes de JavaScript autorizados**:
   ```text
   http://localhost:8080
   ```
6. **URI de redirección autorizados** (deben coincidir exactamente, sin barra
   final). Añade los dos si vas a probar con y sin HTTPS:
   ```text
   http://localhost:8080/login/oauth2/code/google
   https://localhost:8443/login/oauth2/code/google
   ```
7. Pulsar **Crear**. Google muestra el **Client ID** y el **Client secret**.
   Copiarlos en ese momento; el secreto se puede volver a ver después, pero es
   más cómodo guardarlo ya.

## 4. Pegar las credenciales en LibroLog

Editar el archivo `.env` de la raíz del proyecto:

```text
GOOGLE_CLIENT_ID=el-client-id-que-termina-en.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=el-client-secret
```

No hace falta tocar código ni migraciones.

## 5. Reiniciar y comprobar

1. Detener el backend si estaba corriendo.
2. Arrancarlo de nuevo desde IntelliJ o con:
   ```powershell
   cd C:\ruta\a\LibroLog\backend
   $env:JAVA_HOME = "C:\ruta\a\jdk-21"
   .\mvnw.cmd spring-boot:run
   ```
3. Abrir `http://localhost:8080/api/auth/providers`. Debe responder:
   ```json
   {"local":true,"google":true}
   ```
   Antes de configurar las credenciales responde `"google":false`.
4. Abrir `http://localhost:8080/login.html`. Debe aparecer el botón
   **Continuar con Google**.
5. Pulsar el botón, elegir la cuenta añadida como usuario de prueba y aceptar.
6. LibroLog vuelve a `/oauth2-callback.html`, obtiene el token de acceso en
   memoria y abre el panel. La primera vez crea la cuenta con rol `USER` o
   vincula la cuenta local que ya tenga ese mismo correo.

## Si algo falla

- **`redirect_uri_mismatch`**: el URI autorizado no coincide. Debe ser
  exactamente `http://localhost:8080/login/oauth2/code/google`.
- **`Access blocked` / acceso bloqueado**: el correo no está en
  **Usuarios de prueba** de la pantalla de consentimiento.
- **El botón no aparece**: `GOOGLE_CLIENT_ID` o `GOOGLE_CLIENT_SECRET` siguen
  vacíos, o el backend no se reinició después de cambiarlos.
- **La cuenta está deshabilitada**: un administrador debe habilitarla desde
  `/admin.html`; el acceso con Google no salta esa regla.

## Qué se guarda

- Solo se guardan el `sub` de Google y el correo verificado.
- Los tokens de Google no se guardan ni se envían al navegador.
- El refresh token de LibroLog sigue en una cookie `HttpOnly`; el token de
  acceso vive solo en memoria.
