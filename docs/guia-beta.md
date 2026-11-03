# Guía: repartir versiones beta (para el dueño de la app)

Cómo mandar Hilvana a amigos, familia y testers antes de Google Play, e irla actualizando.

## Cómo funciona

1. Publicas una versión con **un comando** (`herramientas\publicar-beta.cmd`).
2. Queda en **GitHub Releases** con el APK adjunto:
   https://github.com/Vega-Mtz-JD/Hilvana-App-/releases/latest
3. Los testers instalan desde ese enlace (instrucciones para ellos: [beta-testers.md](beta-testers.md)).
4. Quien ya tiene Hilvana ve en **Inicio** el aviso "Hay una versión nueva" (la app revisa GitHub
   dos veces al día) y la instala encima sin perder sus notas.

## Publicar una versión

```
herramientas\publicar-beta.cmd 0.10.1 "Arreglé que la burbuja se iba. Nuevo: ordenar tareas."
```

- La versión: tres números y siempre mayor que la anterior.
  - Arreglos pequeños: sube el último número (0.10.0 → 0.10.1).
  - Funciones nuevas: sube el del medio (0.10.1 → 0.11.0).
  - La de Google Play será la 1.0.0.
- El texto es lo que verán los testers ("qué hay de nuevo"). Escríbelo para ellos, sencillo.
- El script se niega si hay cambios sin commit, si no estás en `main`, si la versión ya existe o
  si fallan las pruebas. En esos casos no publica nada.

## ⚠️ La llave de firma: respáldala YA

El APK va firmado con una llave guardada **fuera del proyecto**:

```
C:\Users\Thinkpad\.hilvana\hilvana-release.jks        (la llave)
C:\Users\Thinkpad\.hilvana\keystore.properties        (su contraseña)
```

- **Copia los dos archivos** a un lugar seguro: USB, tu Drive personal o un gestor de contraseñas.
- Si se pierden, **nadie podrá actualizar** su Hilvana: tendrían que desinstalar (y perder sus datos)
  e instalar de nuevo. Con Google Play pasa lo mismo con la "llave de subida".
- Nunca la subas a GitHub (el `.gitignore` ya bloquea `*.jks`).
- Huellas de la llave (para Google Cloud y Play):
  - SHA-1: `37:69:85:00:77:34:E3:44:64:93:9C:22:8E:D9:B3:DD:2E:AD:59:79`
  - SHA-256: `9A:33:AC:79:F0:D0:B2:2A:64:4B:08:E9:FC:E9:CD:17:81:9B:C2:F3:9A:31:B5:92:F6:DC:47:77:ED:7F:F7:03`

## Paso necesario: el respaldo en Drive con la llave nueva

Google solo deja iniciar sesión a apps cuya huella SHA-1 conoce. Hoy solo conoce la de tu laptop
(la de "debug"). Para que **Conectar con Google** funcione en el APK beta:

1. Entra a https://console.cloud.google.com → tu proyecto de Hilvana → **APIs y servicios → Credenciales**.
2. **Crear credenciales → ID de cliente de OAuth → Android**.
3. Nombre: `Hilvana beta`. Paquete: `com.hilvana.app`. SHA-1: el de arriba (`37:69:85:…:59:79`).
4. Guardar. Puede tardar unos minutos en funcionar.

Mientras la pantalla de consentimiento esté en modo "Prueba", agrega en **Pantalla de consentimiento
de OAuth → Usuarios de prueba** el Gmail de cada tester que quiera usar el respaldo en Drive (hasta 100).

## Tu propio celular

Tu Hilvana actual está firmada con la llave de debug: Android **no deja instalar encima** el APK beta.
Una sola vez:

1. En Hilvana: **Ajustes → Respaldo → Respaldar ahora** (Drive) o **Exportar** (archivo).
2. Desinstala Hilvana.
3. Instala el APK beta desde GitHub.
4. **Restaurar** (Drive) o **Importar** (archivo).

Desde ahí, cada versión nueva se instala encima. Si después instalas desde la laptop con
`probar.cmd` (debug), pasa lo mismo al revés: para probar cambios sin publicar, usa
`gradlew assembleRelease` + `adb install -r app\build\outputs\apk\release\app-release.apk`.

## Y Google Play después

Google Play pide, para cuentas personales nuevas, una **prueba cerrada con al menos 12 testers
durante 14 días** antes de publicar. Las pruebas por GitHub **no cuentan** para eso, pero tus testers
de ahora son perfectos para esa etapa: anota el Gmail de cada uno desde ya.
