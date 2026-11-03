# Mejoras de noviembre 2026

Pedidas por el dueño de la app el 2 de noviembre de 2026, después de usar Hilvana en su día a día.
Este archivo sirve de lista de trabajo: se marca cada punto al terminarlo.

Estado: `[ ]` pendiente · `[x]` hecho (subido a `main`) · `[p]` hecho pero falta probarlo en el teléfono.

---

## 1. Título en las notas `[ ]`

**Lo que pidió:** al guardar un link, pegarlo sin amontonar palabras. Quiere un campo de **título** aparte,
que en la nota se vea como encabezado (del link, de la descripción o de la idea).

**Plan:**
- Nueva columna `title` (opcional) en `items` → base de datos v11.
- Panel de captura (burbuja y Compartir): campo "Título (opcional)" encima del texto.
- Editor: campo de título.
- Tarjeta: el título en negrita como encabezado y el texto/link debajo.
- Si no hay título pero el link trae uno (vista previa), se sigue mostrando el del link.
- El título entra en el respaldo (Drive y archivo) y en el buscador.

## 2. Abrir la burbuja desde un widget y desde el mosaico "Capturar" `[ ]`

**Lo que pidió:** un widget de ícono que al tocarlo inicie la burbuja, y que el mosaico del centro de
control también inicie la burbuja (hoy abre el panel de captura).

**Plan:**
- Pantalla invisible `BubbleToggleActivity`: revisa el permiso "Mostrar sobre otras apps", muestra u
  oculta la burbuja y se cierra. Así funciona igual desde el widget y desde el mosaico (Android pide
  que un servicio en primer plano se inicie desde algo que el usuario tocó).
- Widget nuevo 1×1 "Burbuja": círculo con el logo de Hilvana.
- El mosaico pasa a llamarse "Burbuja": se ve encendido cuando la burbuja está visible y al tocarlo
  la muestra u oculta.
- La captura sin burbuja sigue en: Compartir → Hilvana, mantener presionado el ícono y el widget
  Captura rápida.

## 3. Pantalla de inicio más útil (y mejoras en las otras pestañas) `[ ]`

**Lo que pidió:** "Hoy" casi no muestra nada; que sea más eficiente, que muestre cosas como la Bandeja,
y mejorar algo en las demás pestañas.

**Plan:**
- "Hoy" pasa a llamarse **Inicio** y se vuelve un resumen del día:
  - Accesos con contadores: Bandeja, Tareas pendientes, Recordatorios de hoy (tocar → abre esa pestaña).
  - Meta diaria (ya existe).
  - En prioridad (tareas fijadas).
  - Atrasado / Hoy / Próximos 7 días (ya existe).
  - **En la bandeja**: lo último capturado sin revisar (3) + "Ver todo".
  - **Tareas sin fecha** más importantes (3) + "Ver todas".
  - **Notas recientes** (5) + "Ver todas".
- Bandeja: botón "Marcar todo como revisado".
- Tareas y Notas: ordenar (ver punto 5).

## 4. Miniaturas de TikTok `[ ]`

**Lo que pidió:** los links de Facebook muestran miniatura, los de TikTok no.

**Lo que encontré (2 nov 2026):**
- El servicio público de TikTok (oEmbed) **ya no devuelve miniatura ni título**, solo el reproductor.
  Probado con el video de ejemplo de la documentación oficial de TikTok: sin `thumbnail_url`.
- La página de TikTok no entrega las etiquetas de vista previa a apps como Hilvana (las bloquea).
- Los links que comparte la app de TikTok son cortos (`vm.tiktok.com/...`) y redirigen al video.
- No vamos a hacernos pasar por Facebook u otro bot para sacar la imagen: no es legítimo.

**Plan:**
- Seguir el link corto hasta el video para sacar el **@usuario**.
- Si TikTok no da miniatura, mostrar una tarjeta de vista previa propia: "TikTok · @usuario", con el
  ícono de video. Si algún día TikTok vuelve a dar la miniatura, se usa sola.
- Con el título del punto 1 el usuario puede ponerle nombre al video.

## 5. Buscar, ordenar y organizar notas `[ ]`

**Lo que pidió:** buscar y ordenar las notas por título, descripción, categoría, carpeta, etc.

**Plan:**
- Buscador (lupa): también busca en el **título**, la **categoría** y el título del link.
- Filtros nuevos: por **categoría** y por **carpeta**.
- **Ordenar**: más recientes, más antiguas, A–Z (por título/texto) y por importancia.
- En Notas: botón para ordenar la lista "Todas las notas" y las notas dentro de una carpeta.

---

## Orden de trabajo

1. Título (cambia la base de datos; lo usan los puntos 3 y 5).
2. TikTok.
3. Widget y mosaico de la burbuja.
4. Buscar y ordenar.
5. Pantalla de inicio y pestañas.

Después de cada punto: compilar, pruebas, lint, instalar en el teléfono (si está conectado), commit y push.
