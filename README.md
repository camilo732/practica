<div align="center">

<img src="frontend/assets/logo.svg" alt="Logo de LibroLog" width="96">

# LibroLog

**Página web para el registro y seguimiento de hábitos de lectura.**

Biblioteca personal, reseñas estructuradas, metas de lectura y estadísticas,
con los datos de los libros obtenidos automáticamente de Open Library.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5-7952B3?logo=bootstrap&logoColor=white)
![Chart.js](https://img.shields.io/badge/Chart.js-4-FF6384?logo=chartdotjs&logoColor=white)
![i18n](https://img.shields.io/badge/idiomas-ES%20%7C%20EN%20%7C%20DE-0A7EA4)

Proyecto académico · Programación Web · Ingeniería de Software · Universidad Surcolombiana

</div>

---

## Funcionalidades

| | Usuario | |
|---|---|---|
| 🔎 | **Búsqueda de libros** | Título, autoría o ISBN contra Open Library; sin escribir los datos a mano |
| 📚 | **Biblioteca personal** | Pendientes, en lectura y leídos, con progreso por páginas |
| ⭐ | **Reseñas estructuradas** | Estrellas + ritmo, personajes y utilidad |
| 🎯 | **Metas** | Anuales y mensuales, con aviso de si vas a tiempo |
| 📊 | **Estadísticas** | Géneros y autorías más leídos, meses más activos, promedio de páginas por libro |
| 📄 | **Informes y datos** | Informe PDF, exportación CSV/JSON e importación desde Goodreads |
| 🌐 | **Idioma** | Interfaz en español, inglés y alemán |

| | Administrador | |
|---|---|---|
| 👥 | **Gestión de cuentas** | Activar/desactivar, asignar roles, desbloquear y cerrar sesiones |
| 📈 | **Reportes globales de uso** | Solo datos agregados: nunca ve bibliotecas ni reseñas privadas |
| 🧾 | **Auditoría** | Registro de cada acción administrativa |

## Seguridad

- JWT de corta duración + refresh token en cookie `HttpOnly` (solo se guarda su hash).
- Contraseñas con BCrypt, bloqueo por intentos fallidos y límite de peticiones.
- CSP estricta (`script-src 'self'`), HTTPS local opcional e inicio de sesión con Google (OAuth2).
- Ningún secreto en el repositorio: todo se configura en `.env` (ver `.env.example`).

## Inicio rápido

```powershell
git clone <url-del-repositorio>
cd LibroLog
copy .env.example .env      # y editar las claves
cd backend
.\mvnw.cmd spring-boot:run
```

Abrir `http://localhost:8080/` · Documentación de la API: `http://localhost:8080/swagger-ui.html`

---

## Documentación técnica

## Tecnologías

- **Backend:** Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA,
  Spring Security, Flyway, PostgreSQL 18, springdoc-openapi 3.2.0.
- **Frontend:** HTML5, CSS3, Bootstrap 5 y JavaScript moderno (ES modules).
  Sin Angular, por decisión de alcance del proyecto.
- **Pruebas:** JUnit 5, H2 para pruebas de contexto y Testcontainers para
  PostgreSQL en la fase de pruebas de integración.

## Requisitos

- JDK 21.
- Docker Desktop (opcional para desarrollo; necesario para Testcontainers).
- PostgreSQL 18 con una base llamada `librolog_app`.
- Maven no es obligatorio: el proyecto incluye Maven Wrapper.

## Base de datos

La base principal es la que ya existe en pgAdmin:

```text
jdbc:postgresql://localhost:5432/librolog_app
```

Las credenciales se configuran en `.env` (no se sube a Git). Hay una plantilla
en `.env.example`.

Las migraciones viven en `backend/src/main/resources/db/migration`. `V1`, `V2`
y `V3` son inmutables; los cambios nuevos se acumulan en migraciones nuevas.
`V3__german_language.sql` añade el alemán a la restricción de idioma de `users`,
`V4__admin_audit.sql` crea la tabla `admin_audit_logs` del panel de
administración y `V5__book_genre.sql` añade la columna `genre` a `books`.

## Ejecutar en IntelliJ IDEA

1. Abrir `backend/pom.xml` como proyecto Maven.
2. Configurar el SDK del proyecto en JDK 21.
3. Verificar que exista `.env` en la raíz de `LibroLog`.
4. Ejecutar `com.librolog.LibroLogApplication`.
5. Abrir `http://localhost:8080/`.

## Ejecutar desde PowerShell

```powershell
cd C:\ruta\a\LibroLog\backend
$env:JAVA_HOME = "C:\ruta\a\jdk-21"
.\mvnw.cmd spring-boot:run
```

Sustituye `C:\ruta\a\...` por la ruta real del proyecto y `jdk-21` por la
carpeta de tu JDK 21. Si `java` ya está en el `PATH`, puedes omitir
`JAVA_HOME`.

## Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Autenticación (Fase 2)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Crea la cuenta y abre sesión (201) |
| POST | `/api/auth/login` | Inicia sesión (200) |
| POST | `/api/auth/refresh` | Rota el refresh token y devuelve un token nuevo |
| POST | `/api/auth/logout` | Revoca la sesión y borra la cookie (204) |
| GET | `/api/auth/me` | Devuelve el perfil del token actual |

Reglas aplicadas:

- El **token de acceso** (JWT, 15 minutos) viaja en el cuerpo y se usa en
  `Authorization: Bearer <token>`. El cliente debe guardarlo solo en memoria.
- El **refresh token** (7 días) viaja en la cookie `librolog_refresh` con
  `HttpOnly`, `SameSite=Strict`, `Path=/api/auth` y `Secure` cuando hay HTTPS.
  En la base solo se guarda su hash SHA-256.
- Cada refresco invalida el token anterior. Si se reutiliza uno revocado, se
  cierran todas las sesiones del usuario.
- `/refresh` y `/logout` exigen la cabecera `X-LibroLog-Client: web` para
  bloquear peticiones CSRF.
- Tras 5 intentos fallidos la cuenta se bloquea 15 minutos. Todos los errores de
  acceso responden `401` con el mismo mensaje.
- Más de 20 peticiones por minuto y por IP a `/api/auth/**` devuelven `429`.
- Contraseñas con BCrypt coste 12.

## HTTPS local (perfil `dev`)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\generar-keystore-dev.ps1
powershell -ExecutionPolicy Bypass -File scripts\confiar-certificado-dev.ps1   # opcional
cd backend
$env:JAVA_HOME = "C:\ruta\a\jdk-21"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Abre `https://localhost:8443/`. En IntelliJ basta con poner `dev` en **Active
profiles** de la configuración de ejecución (ver la guía completa en
[`docs/https-local.md`](docs/https-local.md)).

- El certificado es autofirmado y sirve para `localhost` y `127.0.0.1` durante
  10 años. El script opcional lo instala en el almacén del usuario actual para
  que Chrome y Edge no muestren el aviso; Firefox tiene su propio almacén.
- En este perfil se rechaza cualquier petición sin cifrar
  (`app.security.require-https=true`), las cookies de refresh llevan `Secure` y
  el CORS solo acepta `https://localhost:8443`.
- **HSTS se desactiva en `dev` a propósito** (`app.security.hsts-enabled=false`):
  con HSTS y un certificado de confianza, el navegador fijaría `localhost` a
  HTTPS durante un año y rompería el arranque normal en
  `http://localhost:8080`, Swagger y otros servicios locales. En producción la
  propiedad vale `true` por defecto.
- Para Google OAuth2 con este perfil, añade también
  `https://localhost:8443/login/oauth2/code/google` a los URI de redirección
  autorizados del cliente en Google Cloud Console.


## Catálogo y biblioteca (Fase 3)

### Catálogo con caché de Open Library

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/books/search?q=&limit=` | Busca en Open Library y guarda el resultado en `books` |
| GET | `/api/books/{id}` | Devuelve un libro de la caché local |

- La búsqueda exige al menos 2 caracteres y devuelve como máximo 40 resultados.
- Cada obra se guarda una sola vez por su `open_library_key` (restricción única):
  repetir una búsqueda no crea duplicados.
- Si Open Library no responde o tarda más de 5 segundos, se devuelve lo que ya
  hay en la caché (`fromCache: true`). Solo si tampoco hay caché se responde
  **503** con `Retry-After`.
- La clave de una obra se valida con la expresión `^/works/OL[A-Za-z0-9]+W$`
  antes de construir ninguna ruta, para impedir inyección de rutas.

### Biblioteca personal

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/library` | Añade un libro (201 + `Location`) |
| GET | `/api/library?status=&page=&size=` | Lista paginada, filtro por estado |
| GET | `/api/library/stats` | Conteo por estado para el panel |
| GET | `/api/library/{id}` | Consulta una entrada propia |
| PUT | `/api/library/{id}` | Cambia estado, páginas o fecha de inicio |
| PATCH | `/api/library/{id}/progress` | Registra la página actual |
| DELETE | `/api/library/{id}` | Elimina la entrada (204) |

Alta de un libro, tres formas:

```json
{ "bookId": 12, "totalPages": 471 }
```

```json
{ "openLibraryKey": "/works/OL27448W" }
```

```json
{ "title": "Libro descatalogado", "author": "Autora", "totalPages": 200 }
```

Reglas:

- El usuario sale siempre del token: no se acepta `userId` en la petición.
  Consultar, modificar o borrar una entrada ajena devuelve **404**, no 403, para
  no revelar que existe.
- Un libro repetido en la misma biblioteca devuelve **409**.
- Los triggers de PostgreSQL calculan el estado y las fechas:
  - `WANT_TO_READ` + progreso > 0 → `READING` y `started_at = hoy`.
  - `current_page = total_pages` → `READ` y `finished_at = hoy`.
  - Cada aumento de `current_page` crea una fila en `reading_logs`.
- Por eso el servicio valida antes de escribir: la página nunca supera el total,
  marcar como pendiente vuelve a la página 0 y volver a `READING` un libro
  terminado retrocede una página.
- `progressPercent` se calcula en el DTO y se limita a 0–100; con total
  desconocido devuelve 0 en lugar de dividir entre cero.
- Los listados usan `EntityGraph` para traer el libro en la misma consulta
  (`open-in-view` está desactivado).

## Reseñas y metas (Fase 4)

### Reseñas estructuradas

| Método | Ruta | Descripción |
|---|---|---|
| PUT | `/api/library/{id}/review` | Crea o reemplaza la reseña |
| GET | `/api/library/{id}/review` | Consulta la reseña de una entrada |
| DELETE | `/api/library/{id}/review` | Elimina la reseña (204) |
| GET | `/api/reviews?page=&size=` | Lista mis reseñas, de la más nueva a la más antigua |

```json
{
  "rating": 5,
  "paceRating": 4,
  "charactersRating": 5,
  "usefulnessRating": 3,
  "content": "Una novela que cambia la forma de leer."
}
```

Reglas:

- `rating` es obligatorio (1–5). `paceRating`, `charactersRating` y
  `usefulnessRating` son opcionales; al reemplazar una reseña, los campos
  omitidos se guardan como `null`, no se conservan.
- Solo se puede reseñar un libro en estado `READ`; si no, **409**. Una reseña ya
  creada se puede editar o borrar aunque el libro se vuelva a abrir.
- Hay una sola reseña por entrada (`uq_reviews_user_book`), así que el alta y la
  modificación comparten `PUT` y conservan el mismo `id`.
- El `content` se recorta y se guarda como `null` si queda vacío.
- La reseña cuelga de la entrada, no del libro: dos personas reseñan la misma
  obra por separado. Una entrada ajena devuelve **404**.

### Metas anuales y mensuales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/goals` | Crea una meta (201 + `Location`) |
| GET | `/api/goals?year=` | Lista mis metas con su avance |
| GET | `/api/goals/{id}` | Consulta una meta |
| PUT | `/api/goals/{id}` | Cambia los objetivos |
| DELETE | `/api/goals/{id}` | Elimina la meta (204) |

```json
{ "period": "ANNUAL", "year": 2026, "targetBooks": 24, "targetPages": 8000 }
```

```json
{ "period": "MONTHLY", "year": 2026, "month": 3, "targetBooks": 2, "targetPages": 600 }
```

Reglas:

- `ANNUAL` no lleva `month` y `MONTHLY` lo exige (1–12); si no coincide, **400**.
- Solo puede existir una meta por usuario y periodo. Los índices parciales
  `uq_goals_annual` y `uq_goals_monthly` lo garantizan en la base y el servicio lo
  comprueba antes para responder **409** con un mensaje claro.
- El avance no se guarda: se recalcula en cada consulta.
  - `booksRead`: libros con `finished_at` dentro del periodo.
  - `pagesRead`: suma de `reading_logs.pages_read` dentro del periodo.
  - `daysTotal`, `daysElapsed` y `daysRemaining` cuentan hoy en ambos extremos,
    de modo que `daysElapsed + daysRemaining = daysTotal + 1`.
  - `expectedPercent` = porcentaje del periodo ya transcurrido.
  - `onTrack` = `booksPercent >= expectedPercent`.
  - `requiredBooksPerMonth` y `requiredPagesPerDay` indican el ritmo que hace
    falta para llegar al objetivo; si el periodo ya terminó, equivalen a lo que
    falta.
- `PUT` solo cambia `targetBooks` y `targetPages`: el periodo no se puede mover.
- El cálculo vive en `GoalPaceCalculator`, una clase pura con `today` inyectable,
  cubierta por `GoalPaceCalculatorTest`.

## Estadísticas, informes e importación (Fase 5)

### Estadísticas de lectura

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/analytics/summary?year=` | Resumen del año: totales, páginas, media, distribución y autorías |
| GET | `/api/analytics/monthly?year=` | Serie de los doce meses |
| GET | `/api/analytics/report.pdf?year=&lang=` | Informe en PDF como adjunto |

Reglas:

- `year` es opcional (año actual) y debe estar entre 2000 y 2100; fuera de rango
  devuelve **400**.
- `finishedThisYear` cuenta los libros con `finished_at` dentro del año y
  `pagesReadThisYear` suma `reading_logs.pages_read` del año.
- `finishedThisMonth` y `pagesReadThisMonth` solo se calculan para el año actual;
  al consultar un año distinto valen `0`.
- `pagesReadTotal` abarca todo el historial y los totales de la biblioteca no
  dependen del año consultado.
- `averageRating` se redondea a un decimal y vale `0.0` cuando no hay reseñas.
- `monthlyActivity` siempre trae los doce meses y `ratingDistribution` las cinco
  calificaciones, aunque estén a cero.
- `busiestMonth` es el mes con más páginas (`0` si no hay actividad).
- `topAuthors` son las cinco autorías con más libros terminados en el año.
- Los cálculos puros viven en `AnalyticsCalculator`, cubierto por
  `AnalyticsCalculatorTest`.

### Informe PDF

- Se genera con OpenPDF en el idioma activo: cabecera `Accept-Language`
  (`es`, `en`, `de`) o parámetro `?lang=es|en|de`; cualquier otro valor devuelve **400**.
- Contenido: usuario, fecha, periodo, resumen general, actividad mensual,
  autorías más leídas y metas del año.
- Se descarga como adjunto: `informe-librolog-<año>.pdf` en español,
  `librolog-report-<año>.pdf` en inglés y `librolog-bericht-<año>.pdf` en alemán.

### Exportación

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/export/csv` | Biblioteca en CSV (UTF-8 con BOM) |
| GET | `/api/export/json` | Copia completa con libros, reseñas y metas |

- El CSV usa la cabecera
  `title,author,status,currentPage,totalPages,startedAt,finishedAt,isbn,publishYear,rating`
  y un archivo exportado se puede volver a importar sin cambios.
- El JSON no incluye contraseñas, tokens ni datos de otras personas.

### Importación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/import/csv` | `multipart/form-data` con el campo `file` |

- Acepta el CSV de LibroLog y el de Goodreads, detectados por la cabecera.
- Máximo **10 MB** y **3000 filas**: un archivo mayor devuelve **413** y uno con
  demasiadas filas, **400**.
- Cada fila se importa en su propia transacción: un error no deshace el resto.
- Los libros repetidos (mismo ISBN o mismo título y autoría) se omiten y se
  cuentan en `skipped`.
- Las calificaciones de los libros terminados se convierten en reseñas.
- Las fechas se respetan: un libro importado como `READ` conserva su
  `finishedAt` y la importación no inventa páginas leídas.
- La respuesta indica `format`, `totalRows`, `importedBooks`, `importedReviews`,
  `skipped`, `errorCount` y hasta 50 errores detallados.
- Codificación: UTF-8 con o sin BOM; si el archivo no es UTF-8 válido se prueba
  ISO-8859-1.

```powershell
curl.exe -X POST http://localhost:8080/api/import/csv `
  -H "Authorization: Bearer $token" `
  -F "file=@goodreads_library_export.csv"
```

## Frontend completo (Fase 6)

La aplicación es multipágina: cada pantalla es un documento HTML real con sus
propios enlaces, sin SPA ni enrutador de JavaScript.

### Páginas

| Página | Archivo | Contenido |
|---|---|---|
| Inicio | `/index.html` | Presentación, estado de la API y accesos a sesión |
| Entrar | `/login.html` | Inicio de sesión y destino `next` |
| Registro | `/register.html` | Alta con idioma preferido `es`, `en` o `de` |
| Panel | `/dashboard.html` | Totales, actividad mensual, autorías, metas y PDF |
| Biblioteca | `/library.html` | Filtros, alta manual, progreso, edición y borrado |
| Catálogo | `/catalog.html` | Búsqueda en Open Library y alta con un clic |
| Reseñas | `/reviews.html` | Alta, edición y borrado de reseñas propias |
| Metas | `/goals.html` | Metas anuales y mensuales con gráfico y tabla |
| Datos | `/data.html` | Exportación CSV/JSON e importación con jQuery |
| Perfil | `/profile.html` | Datos de la cuenta, idioma y estado de la sesión |
| Administración | `/admin.html` | Cuentas, roles, bloqueos, sesiones y auditoría (solo ADMIN) |
| Retorno OAuth2 | `/oauth2-callback.html` | Cierra el viaje de Google y abre la sesión |

### Comportamiento y accesibilidad

- Bootstrap 5 y Chart.js se sirven desde `/vendor`: no hay CDN ni scripts en
  línea. La política CSP usa `script-src 'self'`.
- La navegación y las tablas se generan con módulos ES compartidos
  (`api.js`, `i18n.js`, `layout.js`, `ui.js`, `charts.js`, `forms.js`).
- Los formularios muestran errores por campo, resumen accesible y ayudas
  asociadas con `aria-describedby`.
- Los diálogos usan `<dialog>` nativo, se cierran con `Escape` y devuelven el
  foco al botón que los abrió; los avisos se anuncian en una región `aria-live`.
- Los gráficos tienen una tabla equivalente y respetan `prefers-reduced-motion`.
- El diseño es responsive y se probó a 360, 768 y 1366 píxeles.

### Idiomas

- Tres idiomas completos: español (`es.js`), inglés (`en.js`) y alemán
  (`de.js`), con las mismas 593 claves.
- El cambio es inmediato, se guarda en `localStorage` (`librolog.language`) y se
  anuncia con el evento `librolog:languagechange` para volver a pintar los datos.
- Al iniciar sesión o registrarse, el idioma del formulario se envía a la API.
  La migración `V3__german_language.sql` admite `de` en la base de datos.
- `Accept-Language` viaja en cada petición; las fechas, números y meses usan
  `Intl` con el locale activo (`es-ES`, `en-GB`, `de-DE`).
- El informe PDF también se genera en alemán (`librolog-bericht-<año>.pdf`).

### Seguridad del frontend

- El token de acceso vive solo en memoria; el refresh token sigue en una cookie
  `HttpOnly` y nunca se lee desde JavaScript.
- Las páginas privadas comprueban la sesión antes de pintar datos, pero la
  protección real está en la API: cada endpoint valida el token y la propiedad
  del recurso.
- CORS es estricto, no se aceptan credenciales desde otros orígenes y las
  cabeceras incluyen CSP, `X-Frame-Options: DENY`, `Referrer-Policy` y HSTS en
  HTTPS.
- El aviso del servidor solo se muestra cuando la interfaz está en español y no
  hay errores de campo; así no se filtran detalles internos.

### Comprobar la fase

```powershell
cd C:\ruta\a\LibroLog\backend
$env:JAVA_HOME = "C:\ruta\a\jdk-21"
.\mvnw.cmd test
.\mvnw.cmd javadoc:javadoc
.\mvnw.cmd spring-boot:run
```

Después, abrir `http://localhost:8080/` y recorrer:

1. Registro con idioma alemán y comprobación de que el perfil guarda `de`.
2. Cambio es → en → de desde la cabecera, recarga y persistencia.
3. Biblioteca: alta manual, progreso hasta 100 %, edición y borrado.
4. Catálogo: búsqueda, alta y aviso de libro repetido.
5. Reseñas: creación, edición, borrado y prellenado con `?book=<id>`.
6. Metas: alta anual y mensual, edición, borrado y cambio de año.
7. Datos: exportación CSV/JSON, plantilla, importación válida y con errores.
8. Panel: estadísticas, gráficos, tablas equivalentes y PDF en los tres idiomas.
9. Perfil: datos, selector de idioma y cierre de sesión.

## Administración y acceso con Google (Fase 7)

### Panel de administración

La pantalla `/admin.html` solo es visible y utilizable con el rol `ADMIN`. El
enlace aparece en la cabecera y en el perfil únicamente para esas cuentas; la
API vuelve a comprobar el rol en cada petición.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/admin/stats` | Totales de cuentas, administradores, bloqueos y proveedores |
| GET | `/api/admin/users` | Lista con `q`, `role`, `status`, `page` y `size` |
| GET | `/api/admin/users/{id}` | Detalle administrativo de una cuenta |
| PATCH | `/api/admin/users/{id}/status` | Habilita o deshabilita (`enabled`) |
| PATCH | `/api/admin/users/{id}/role` | Concede o retira `ADMIN` (`admin`) |
| POST | `/api/admin/users/{id}/unlock` | Reinicia intentos fallidos y el bloqueo |
| POST | `/api/admin/users/{id}/sessions/revoke` | Cierra todas las sesiones |
| GET | `/api/admin/audit` | Auditoría paginada, de más reciente a más antigua |

Reglas de seguridad del panel:

- Un administrador no puede deshabilitarse ni quitarse `ADMIN` a sí mismo.
- El sistema no permite quedarse sin ningún administrador activo (`409`).
- Deshabilitar una cuenta revoca sus sesiones y el siguiente acceso falla.
- Cada cambio queda en `admin_audit_logs` con actor, cuenta afectada, acción,
  valor anterior, valor nuevo y fecha. Las sesiones se identifican por su
  `username`, no por datos sensibles.
- Los botones de la propia cuenta aparecen deshabilitados en la interfaz, pero
  la decisión final siempre está en el backend.

### Primer administrador

No hay correos escritos en el código ni en las migraciones. Al arrancar,
`AdminBootstrap` concede `ADMIN` a las cuentas ya registradas que aparezcan en
`APP_ADMIN_EMAILS` (separadas por comas). Es idempotente y no crea usuarios.

```text
APP_ADMIN_EMAILS=tu-correo@ejemplo.com
```

Después de arrancar, cerrar y volver a abrir sesión para que el token incluya el
rol nuevo. El filtro JWT recarga el usuario en cada petición, así que también se
puede comprobar sin reiniciar el servidor.

### Acceso con Google (OAuth2)

El botón de Google solo aparece cuando el backend tiene credenciales. Si no las
hay, el acceso local sigue funcionando sin errores.

1. Crear credenciales OAuth 2.0 de tipo **Aplicación web** en Google Cloud
   Console.
2. Añadir como URI de redirección autorizado:
   `http://localhost:8080/login/oauth2/code/google`.
3. Copiar los valores en `.env`:

```text
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

4. Reiniciar el backend. `GET /api/auth/providers` debe devolver
   `{"local":true,"google":true}`; con credenciales vacías devuelve
   `google:false`.

La guía detallada, con el paso a paso de Google Cloud Console, está en
[`docs/google-oauth.md`](docs/google-oauth.md).

Flujo implementado:

- `GET /api/auth/oauth2/google?next=/dashboard.html` guarda el destino en una
  cookie temporal y redirige a Google.
- Al volver, `GoogleOAuthSuccessHandler` valida la cuenta, crea o vincula el
  usuario, emite el refresh token en la cookie `HttpOnly` y redirige a
  `/oauth2-callback.html`. Esa página llama a `refreshSession()` para obtener el
  token de acceso en memoria y continúa al destino.
- Se exige correo verificado. Primero se busca por `sub` de Google y después por
  correo; si existe una cuenta local con ese correo, se vincula sin cambiar la
  contraseña. Si no existe, se crea con rol `USER` y un nombre de usuario único.
- Las cuentas deshabilitadas no pueden entrar con Google. Los tokens de Google
  nunca se devuelven al navegador ni se guardan.

### Comprobar la fase

```powershell
cd C:\ruta\a\LibroLog\backend
$env:JAVA_HOME = "C:\ruta\a\jdk-21"
.\mvnw.cmd test
.\mvnw.cmd javadoc:javadoc
.\mvnw.cmd spring-boot:run
```

En `http://localhost:8080/`:

1. Entrar con la cuenta indicada en `APP_ADMIN_EMAILS` y comprobar el enlace
   **Administración** en la cabecera.
2. Ver los totales, buscar por texto, filtrar por rol y estado, y paginar.
3. Abrir el detalle de una cuenta, deshabilitarla, comprobar que no puede entrar,
   volver a habilitarla y desbloquearla.
4. Conceder `ADMIN` a otra cuenta, comprobar que ve el panel y retirarlo después.
5. Revocar las sesiones de una cuenta y verificar que su siguiente refresco falla.
6. Revisar que cada acción aparece en la tabla de auditoría.
7. Sin credenciales de Google, comprobar que el botón no aparece y que el acceso
   local funciona. Con credenciales, entrar con Google y verificar que se crea o
   vincula la cuenta y que se llega a `/oauth2-callback.html`.
8. Abrir Swagger y comprobar que todas las rutas nuevas están documentadas.

## Requisitos del anteproyecto

Completa los puntos del anteproyecto que faltaban:

| Requisito | Implementación |
|---|---|
| Géneros más leídos | `books.genre` (V5) se llena con el primer tema útil de Open Library; `GET /api/analytics/summary` devuelve `topGenres` y el panel muestra la lista |
| Promedio de páginas por libro | `averagePagesPerBook` en el resumen y tarjeta en el panel |
| Reportes globales de uso (ADMIN) | `GET /api/admin/usage`: libros registrados, libros por usuario, reseñas, % de usuarios con meta, usuarios activos y páginas del mes |
| El ADMIN no ve contenido privado | El reporte solo usa `count`/`sum`; no devuelve títulos, bibliotecas ni reseñas |

Los libros que ya estaban en la caché antes de V5 no tienen género: se
completa la próxima vez que aparezcan en una búsqueda del catálogo.

## PostgreSQL alternativo con Docker

El archivo `docker-compose.yml` levanta PostgreSQL 18 en el puerto **5433** con
la misma base `librolog_app`. Sirve para otro equipo o para no instalar
PostgreSQL localmente. No reemplaza la base de pgAdmin.

```powershell
docker compose up -d db
```

Para usar esa base, cambiar en `.env`:

```text
DB_URL=jdbc:postgresql://localhost:5433/librolog_app
```

## ¿Para qué se usa Docker en este proyecto?

- **Testcontainers:** en la fase de pruebas se levanta un PostgreSQL temporal
  dentro de Docker para comprobar los triggers sin tocar la base real.
- **Entorno reproducible:** `docker-compose.yml` permite ejecutar el proyecto
  en otro computador con la misma versión de PostgreSQL.
- **Aislamiento:** la base de pruebas se destruye al terminar; no se mezcla con
  los datos de `librolog_app`.

## Estructura

```text
backend/   API REST y migraciones Flyway
frontend/  HTML, CSS y JavaScript
docs/      Documentación técnica y del proyecto
```

## Fases del proyecto

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Entorno, proyecto Maven y configuración base | Completada |
| 1 | Base de datos PostgreSQL y migraciones Flyway | Completada |
| 2 | Autenticación JWT, refresh token, bloqueos, límites y HTTPS | Completada |
| 3 | Catálogo Open Library y biblioteca personal | Completada |
| 4 | Reseñas y metas de lectura | Completada |
| 5 | Estadísticas, informe PDF, exportación e importación | Completada |
| 6 | Frontend multipágina, Chart.js, accesibilidad e i18n ES/EN/DE | Completada |
| 7 | Administración de cuentas, auditoría y Google OAuth2 | Completada |
| 8 | Entrega: repositorio seguro en GitHub, documentación y verificación final | En curso |

## Estado

Las fases 0 a 7 están cerradas. La Fase 8 prepara la entrega: repositorio sin
secretos ni datos personales, README y documentación, y comprobación final de
pruebas y Javadoc.
