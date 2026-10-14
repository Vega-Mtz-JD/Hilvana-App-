# Guía de desarrollo diario (sin Android Studio)

Programar, probar en el celular por Wi-Fi y guardar en GitHub, gastando lo
mínimo de la laptop.

> Todos los comandos se escriben en la terminal (Warp o PowerShell) **dentro de
> la carpeta del proyecto**:
>
> ```
> cd C:\Users\Thinkpad\Proyectos\prueba\Hilvana-App-
> ```

---

## 1. Al empezar a trabajar

**En el celular**
1. Conéctalo a la **misma red Wi-Fi** que la laptop.
2. *Ajustes → Ajustes adicionales → Opciones de desarrollador →*
   **Depuración inalámbrica → activar**.

**En la laptop**
1. **Cierra Android Studio** si está abierto (ahorra ~2 GB de RAM).
2. Trae lo último de GitHub (por si cambiaste algo desde otra PC):
   ```
   git pull
   ```
3. Conecta el celular:
   ```
   .\herramientas\conectar.cmd
   ```
   Debe decir `Conectado: 192.168.x.x:xxxxx` en verde.

## 2. Probar cada cambio en el celular

```
.\herramientas\probar.cmd
```

Hace todo solo: conecta → compila → instala → abre Hilvana en el celular →
apaga Gradle para liberar memoria. Tarda ~30 segundos.

¿Vas a probar muchas veces seguidas? Usa esta variante: no apaga Gradle entre
pruebas, así las siguientes compilaciones son más rápidas (pero ocupa ~3 GB de
RAM mientras tanto):

```
.\herramientas\probar.cmd -SinApagar
```

Al terminar la tanda, apágalo tú:

```
cd app-android
.\gradlew.bat --stop
cd ..
```

## 3. Al terminar de trabajar

1. Guarda en GitHub (sección 5).
2. Desconecta el celular:
   ```
   adb disconnect
   ```
3. En el celular apaga **Depuración inalámbrica** (opcional: se apaga sola al
   reiniciar o cambiar de Wi-Fi). Deja encendida "Depuración USB", no molesta.

---

## 4. Problemas comunes

| Problema | Solución |
|---|---|
| `No encontré tu celular` | Misma Wi-Fi + pantalla del celular encendida + apaga y prende "Depuración inalámbrica". |
| Sigue sin encontrarlo | Entra a "Depuración inalámbrica" en el celular, mira **Dirección IP y puerto** y ejecuta `.\herramientas\conectar.cmd 192.168.1.2:41234` (con tus números). El puerto **cambia** cada vez que activas la depuración. |
| `failed to authenticate` / pide emparejar | Se perdió el emparejamiento. En el celular: *Depuración inalámbrica → Vincular dispositivo con código*. En la laptop: `adb pair IP:PUERTO_DE_VINCULACION` y escribe el código. Luego `conectar.cmd`. |
| `INSTALL_FAILED_USER_RESTRICTED` (Xiaomi) | Activa **"Instalar vía USB"** en Opciones de desarrollador y acepta el aviso que aparece en el celular. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | La app instalada se firmó en otra PC. Desinstálala: `adb uninstall com.hilvana.app` y vuelve a `probar.cmd` (se borran los datos de prueba). |
| La app se cierra sola | Mira el error: `adb logcat -d -s AndroidRuntime:E` |
| La burbuja desaparece tras un rato | Xiaomi la cerró por batería: *Ajustes → Apps → Hilvana → Ahorro de batería → **Sin restricciones*** y activa **Inicio automático**. |
| `adb` no se reconoce | Cierra y abre la terminal (o reinicia la laptop) para que cargue las variables de entorno. |

---

## 5. Guardar en GitHub

Trabajamos en la rama **`fase-0-prototipo`**. Comprueba en cuál estás:

```
git branch --show-current
```

### Guardar avances (lo normal, cada vez que algo funciona)

```
git status
git add -A
git commit -m "Describe en una línea qué cambiaste"
git push
```

- `git status` → muestra qué archivos cambiaron (revísalo antes de guardar).
- `git add -A` → marca todos los cambios para guardar.
- `git commit -m "..."` → guarda una "foto" del proyecto en tu laptop.
- `git push` → sube esas fotos a GitHub.

### Ver el historial

```
git log --oneline -10
```

### Deshacer cambios que todavía NO guardaste en un commit

```
git restore ruta\del\archivo.kt     # un archivo
git restore .                       # TODO (cuidado: no se puede recuperar)
```

### Los "problemas" que tuvimos con Git (ya resueltos)

- **Git no se reconocía** justo después de instalarlo → la terminal necesitaba
  reiniciarse para ver el nuevo PATH.
- **Faltaba tu identidad** (nombre/correo del autor) → ya configurada en este
  repositorio con tu correo privado de GitHub:
  `git config user.name` / `git config user.email`.
- **Había dos versiones del proyecto**: `main` (estructura del 10-oct, casi todo
  vacío) y `fase-0-prototipo` (la que funciona, en `app-android/`). Seguimos con
  `fase-0-prototipo`; `main` queda solo como referencia.

---

## 6. Resumen rápido

```
# empezar
git pull
.\herramientas\conectar.cmd

# probar (repetir cada vez que cambie el código)
.\herramientas\probar.cmd

# guardar
git add -A
git commit -m "qué cambié"
git push

# terminar
adb disconnect
```
