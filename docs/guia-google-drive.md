# Configurar Google (cuenta + respaldo en Google Drive)

Hilvana guardará tu respaldo `.zip` (notas, imágenes y audios) en una **carpeta
oculta de tu propio Google Drive** (`appDataFolder`): solo Hilvana puede verla,
no aparece entre tus archivos y usa tu espacio gratuito de Google.

Para que Google permita esto, hay que registrar la app **una sola vez** en
Google Cloud. Es gratis y **no pide tarjeta**. Toma unos 10 minutos.

> Usa la misma cuenta de Google con la que vas a iniciar sesión en el celular.

---

## 1. Crear el proyecto

1. Entra a <https://console.cloud.google.com/> y acepta los términos si te los pide.
2. Arriba a la izquierda, junto al logo, toca el **selector de proyecto** →
   **Proyecto nuevo**.
3. Nombre: `Hilvana` → **Crear**. Espera unos segundos y **selecciónalo**.

## 2. Activar la API de Google Drive

1. Menú ☰ → **APIs y servicios** → **Biblioteca**.
2. Busca **Google Drive API** → ábrela → **Habilitar**.

## 3. Pantalla de consentimiento (lo que verás al iniciar sesión)

Menú ☰ → **APIs y servicios** → **Pantalla de consentimiento de OAuth**
(puede aparecer como **Google Auth Platform**) → **Comenzar**:

1. **Información de la app**: nombre `Hilvana`, correo de asistencia: tu correo.
2. **Público**: **Externo**.
3. **Información de contacto**: tu correo → **Crear**.

Luego, en el menú de la izquierda de esa sección:

4. **Acceso a datos** (*Data access*) → **Agregar o quitar permisos** → busca y
   marca:
   - `.../auth/drive.appdata` (ver y administrar su propia configuración en Google Drive)
   - `openid`, `.../auth/userinfo.email`, `.../auth/userinfo.profile`

   → **Actualizar** → **Guardar**.
5. **Público** (*Audience*) → en **Usuarios de prueba** → **Agregar usuarios** →
   escribe **tu correo de Gmail** → Guardar.
   *(Mientras la app está "en prueba", solo esos correos pueden iniciar sesión.
   Al publicar en Play se pasa a "en producción".)*

## 4. Registrar la app Android

Menú de la izquierda → **Clientes** (*Clients*) → **Crear cliente**:

| Campo | Valor |
|---|---|
| Tipo de aplicación | **Android** |
| Nombre | `Hilvana Android (depuración)` |
| Nombre del paquete | `com.hilvana.app` |
| Huella digital SHA-1 | `42:C3:B5:65:EC:03:D8:24:72:64:0A:20:6F:D8:9F:29:30:CA:32:9D` |

→ **Crear**. No hace falta descargar nada ni copiar ningún secreto: Google
reconoce la app por el nombre del paquete + la huella.

> Esa huella es la de la **llave de depuración de tu laptop** (la que usa
> `probar.cmd`). Cuando publiquemos en Google Play habrá que agregar otro cliente
> Android con la huella de la llave de Play.

## 5. Listo

Avísale a Claude **"ya está lo de Google Cloud"** y se conecta el inicio de
sesión y el respaldo en Drive.

---

### Si algo sale mal

| Mensaje | Qué revisar |
|---|---|
| "Acceso bloqueado: la app no completó la verificación" | Tu correo no está en **Usuarios de prueba** (paso 3.5). |
| Error 10 / `DEVELOPER_ERROR` | El **paquete** o la **SHA-1** del paso 4 no coinciden exactamente. |
| "Esta app no está verificada" | Normal en modo prueba: **Continuar**. |
