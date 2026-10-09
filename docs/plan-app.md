# Hilvana — Plan de la app (v0.2)

> App Android "todo en uno" para notas, tareas, ideas y recordatorios, con una
> **burbuja flotante** siempre a mano para capturar en segundos, y **widgets**
> más claros que los de la competencia.

---

## 1. La idea en una frase

**"Que nada se te olvide: lo anotas en 2 segundos desde cualquier app, y Hilvana
lo ordena por ti."**

El problema real no es *organizar*, es **capturar a tiempo**. Las apps actuales
(Google Keep, Todoist, TickTick, Microsoft To Do, Samsung Notes) obligan a salir
de lo que estás haciendo, buscar la app, abrirla y elegir dónde guardar. En ese
camino la idea se pierde. Hilvana elimina ese camino.

## 2. Para quién es

- Personas ocupadas que tienen ideas o pendientes "en movimiento" (trabajo,
  escuela, casa).
- Estudiantes que necesitan recordar entregas y apuntes rápidos.
- Gente olvidadiza que hoy se manda mensajes a sí misma por WhatsApp o usa
  capturas de pantalla como recordatorio (¡competencia real!).

## 3. Qué la hace distinta (propuesta de valor)

| # | Diferenciador | Por qué importa |
|---|---|---|
| 1 | **Burbuja flotante** encima de todas las apps | Captura sin salir de lo que haces |
| 2 | **Captura inteligente**: escribes "mañana 5pm llamar a mamá" y se crea un recordatorio solo | No tienes que elegir tipo, fecha ni carpeta |
| 3 | **Bandeja de entrada única**: todo cae en un solo lugar y luego se ordena | Cero fricción al capturar |
| 4 | **Widgets bien diseñados e interactivos** (marcar tareas, ver el día, capturar) | La mayoría de widgets actuales son feos o solo abren la app |
| 5 | **Funciona sin internet** | Nunca pierdes una nota por falta de señal |

> ⚠️ Ojo: ya existen apps de "notas flotantes" (y Samsung/Xiaomi tienen ventanas
> flotantes). La burbuja sola **no basta** como diferenciador; lo que gana es la
> combinación **burbuja + captura inteligente + buen diseño**.

---

## 4. Cómo funcionaría (flujo principal)

```
 Usuario en cualquier app (WhatsApp, YouTube, navegador...)
            │
            ▼
   [ Toca la burbuja flotante ]
            │
            ▼
 ┌───────────────────────────────┐
 │  Mini-panel de captura        │
 │  [ Escribe o dicta...    🎤 ] │
 │  Chips: Nota · Tarea · Idea · │
 │         Recordatorio          │
 │  [ Guardar ]                  │
 └───────────────────────────────┘
            │  (se cierra solo, vuelves a tu app)
            ▼
   Hilvana detecta fecha/hora → si la hay, programa recordatorio
            │
            ▼
   Se guarda en "Bandeja" → luego el usuario lo ordena (o se auto-clasifica)
```

### Gestos de la burbuja
- **Toque**: abre el mini-panel de captura de texto.
- **Mantener presionado**: grabar nota de voz (suelta para guardar).
- **Arrastrar**: moverla; se pega al borde de la pantalla.
- **Arrastrar a la "X" inferior**: ocultarla por un rato.
- **Doble toque** (opcional): abrir la app completa.

### Pantallas de la app

1. **Hoy** — lo que vence hoy + recordatorios próximos + tareas atrasadas.
2. **Bandeja** — todo lo capturado sin ordenar.
3. **Notas / Ideas** — con etiquetas, colores y búsqueda.
4. **Tareas** — listas, subtareas, prioridad, fecha, repetición.
5. **Calendario** — vista semanal/mensual de tareas y recordatorios.
6. **Buscar** — búsqueda global instantánea.
7. **Ajustes** — burbuja (tamaño, transparencia, posición, apps donde ocultarla),
   tema, respaldo, notificaciones.

### Tipos de elemento (un solo modelo de datos)

Todo es un **"Elemento"** con campos opcionales; el "tipo" solo cambia cómo se
muestra. Así se puede convertir una idea en tarea, o una nota en recordatorio,
con un toque.

| Campo | Nota | Idea | Tarea | Recordatorio |
|---|---|---|---|---|
| Texto / título | ✅ | ✅ | ✅ | ✅ |
| Etiquetas / color | ✅ | ✅ | ✅ | ✅ |
| Checklist / hecho | opcional | – | ✅ | ✅ |
| Fecha / hora | – | – | opcional | ✅ |
| Repetición | – | – | opcional | opcional |
| Adjuntos (foto, audio) | ✅ | ✅ | opcional | – |

---

## 5. Widgets propuestos

Hechos con **Jetpack Glance** (widgets con Compose, interactivos desde Android 12).

| Widget | Tamaño | Qué hace |
|---|---|---|
| **Captura rápida** | 2×1 / 4×1 | Barra "¿Qué no quieres olvidar?" + botón 🎤 |
| **Mi día** | 4×2 / 4×4 | Tareas de hoy con casillas para marcar sin abrir la app |
| **Próximo recordatorio** | 2×2 | Cuenta regresiva grande y clara al siguiente recordatorio |
| **Nota fijada** | 2×2 / 4×2 | Muestra una nota/lista elegida (ej. lista del súper) |
| **Semana** | 4×2 | Mini-calendario con puntos por día con pendientes |

Principios de diseño de widgets:
- Texto grande y legible, máximo 1 acción principal por widget.
- Respetan colores del sistema (Material You / colores dinámicos) y modo oscuro.
- Se actualizan al instante al marcar algo.
- Estados vacíos amables ("Nada pendiente hoy 🎉") en vez de un widget en blanco.

---

## 6. Tecnología recomendada

**Recomendación: nativo con Kotlin + Jetpack Compose.**

La burbuja flotante, los widgets, las alarmas exactas y los servicios en
primer plano son funciones **muy específicas de Android**. Con Flutter o React
Native habría que escribirlas en Kotlin de todos modos, así que conviene hacerlo
nativo desde el inicio. (Si en el futuro quieren iPhone: en iOS **no existe** la
burbuja flotante, sería otra estrategia de producto.)

| Pieza | Tecnología |
|---|---|
| Lenguaje / UI | Kotlin + Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Clean-ish (capas ui / domain / data), multi-módulo ligero |
| Base de datos local | Room (SQLite) — *offline-first* |
| Búsqueda | Room FTS (búsqueda de texto completo) |
| Inyección de dependencias | Hilt |
| Burbuja flotante | `WindowManager` + `TYPE_APPLICATION_OVERLAY` dentro de un *Foreground Service* |
| Widgets | Jetpack Glance |
| Recordatorios | `AlarmManager` (alarmas exactas) + notificaciones |
| Tareas en segundo plano | WorkManager (respaldos, limpieza, sync) |
| Voz a texto | `SpeechRecognizer` de Android (gratis, en el dispositivo cuando se puede) |
| Fechas en lenguaje natural | Parser propio en español para empezar ("mañana", "el viernes a las 5", "en 2 horas") |
| Inicio de sesión con Google | Credential Manager (botón "Continuar con Google") + Firebase Auth |
| Respaldo/sincronización en la nube | Firebase Firestore (solo para cuentas Google) |
| Versión mínima | Android 8.0 (API 26) — cubre >95% de dispositivos |

> **Pensando en iPhone a futuro:** la lógica (modelo de datos, parser de fechas,
> reglas de recordatorios) se escribe en Kotlin puro, separada de la interfaz.
> Así, cuando llegue iOS, se puede reutilizar con **Kotlin Multiplatform** en vez
> de reescribirla. Firebase también funciona en iOS, así que las cuentas Google
> servirán en ambos.

---

## 6.1 Cuentas: dos modos

Al abrir la app por primera vez, el usuario elige:

| | **Cuenta Google** | **Perfil local** |
|---|---|---|
| Cómo entra | Botón "Continuar con Google" | Escribe su nombre (y opcionalmente una foto/avatar) |
| Dónde viven sus datos | En el teléfono **y** respaldados en la nube | Solo en el teléfono |
| Si cambia de teléfono | Inicia sesión y recupera todo | Debe exportar/importar un respaldo manual |
| Necesita internet | Solo para sincronizar (la app funciona sin señal) | Nunca |

Reglas importantes:
- **Ambos modos guardan todo localmente primero** (Room). La nube es una copia,
  no la fuente principal → la app es igual de rápida en los dos modos.
- **Pasar de local a Google** debe ser fácil: "Conectar mi cuenta Google" en
  Ajustes sube todas sus notas sin perder nada. (Es la forma natural de que los
  usuarios locales terminen respaldando.)
- Cerrar sesión de Google pregunta si conservar una copia local.
- Opción de **bloqueo con huella/PIN** para ambos modos (las notas son privadas).
- Eliminar cuenta y datos desde la app (Google Play lo exige si hay cuentas).

---

## 7. ⚠️ Puntos críticos a considerar (lo más importante de este documento)

### 7.1 La burbuja NO puede venir activada "por defecto"
Android exige que el usuario dé el permiso **"Mostrar sobre otras apps"**
(`SYSTEM_ALERT_WINDOW`) **manualmente** desde Ajustes. Ninguna app lo puede
activar sola.

➡️ Solución: un **onboarding muy bueno** al abrir la app por primera vez:
pantalla que explica *por qué* (con animación), botón que lleva directo al
ajuste, y detección de cuando el usuario regresa con el permiso dado.

### 7.2 Mantener la burbuja viva (batería y fabricantes)
- La burbuja vive dentro de un **servicio en primer plano**, que obliga a mostrar
  una **notificación permanente**. Hay que hacerla útil (ej. "Toca para capturar"
  + botón ocultar burbuja) en vez de molesta.
- Android 14+ exige declarar el **tipo** de servicio; probablemente `specialUse`,
  que requiere **justificarlo ante Google Play** al publicar.
- Fabricantes como **Xiaomi, Huawei, Oppo, Vivo y Samsung** matan apps en segundo
  plano agresivamente. Habrá que guiar al usuario a desactivar la optimización de
  batería para Hilvana (ver dontkillmyapp.com).
- Reiniciar la burbuja al encender el teléfono (`BOOT_COMPLETED`).

### 7.3 Restricciones del sistema sobre la burbuja
- No se muestra en la pantalla de bloqueo ni sobre ciertas pantallas del sistema
  (ajustes de permisos, etc.). Es normal.
- Desde Android 12 las ventanas superpuestas que bloquean toques están
  restringidas: la burbuja debe ser **pequeña** y solo capturar toques en su área.
- Opción para **ocultarla automáticamente** en apps a pantalla completa (juegos,
  videos) — mucha gente se quejaría si no.
- Algunos teléfonos de gama baja (Android Go) no permiten superposiciones.

### 7.4 Recordatorios que SÍ suenen a tiempo
- Android 13+: permiso de **notificaciones** (`POST_NOTIFICATIONS`) en tiempo de
  ejecución.
- Android 14+: el permiso de **alarmas exactas** (`SCHEDULE_EXACT_ALARM`) viene
  **denegado por defecto** en instalaciones nuevas; hay que pedirlo y explicar.
  (`USE_EXACT_ALARM` solo lo permite Play a apps de reloj/calendario.)
- Volver a programar todas las alarmas al reiniciar el teléfono o cambiar la hora.

### 7.5 Plan B de captura (por si el usuario no da el permiso de burbuja)
No todo el mundo aceptará la burbuja. La app debe seguir siendo genial sin ella:
- **Widget de captura rápida** en la pantalla de inicio.
- **Mosaico en Ajustes rápidos** (Quick Settings Tile) — bajas la barra y tocas.
- **Accesos directos** al mantener presionado el ícono de la app.
- **Compartir a Hilvana** desde cualquier app (textos, links, imágenes).
- Notificación persistente opcional con botón "Capturar".

### 7.6 Privacidad y Google Play
- Las notas son datos **muy personales**: guardarlas localmente por defecto,
  cifrar respaldos, nada de anuncios que lean contenido.
- Requisitos de Play: **política de privacidad** publicada, formulario de
  **Seguridad de los datos**, justificación de permisos sensibles (superposición,
  servicio en primer plano, alarmas exactas).
- Cuenta de desarrollador de Google Play (pago único de USD 25). Las cuentas
  personales nuevas deben hacer una **prueba cerrada con testers durante 14 días**
  antes de publicar en producción.

### 7.7 Respaldo de datos
Si el usuario pierde o cambia el teléfono y pierde sus notas, la app muere.
Desde el MVP: **respaldo en la nube** para cuentas Google, y para perfiles
locales **exportar/importar** (archivo) + respaldo automático de Android.

---

## 8. Alcance por fases

> Tiempos ajustados para alguien con experiencia en web/software pero **nuevo en
> Android**. Son estimaciones trabajando a ritmo constante; lo importante es el
> orden, no la fecha.

### Fase A — Aprender lo necesario de Android (2–3 semanas)
Ya sabes programar, así que no es empezar de cero: es aprender un "dialecto".

| Lo que ya conoces (web) | Su equivalente en Android |
|---|---|
| JavaScript / TypeScript | **Kotlin** (muy parecido a TypeScript) |
| Componentes de React / Vue | **Composables** de Jetpack Compose |
| `useState` / estado reactivo | `remember`, `StateFlow`, ViewModel |
| npm / package.json | **Gradle** / `build.gradle.kts` |
| SQL / base de datos local | **Room** (SQLite con anotaciones) |
| Rutas (React Router) | Navigation Compose |
| Service workers / tareas en segundo plano | Services, WorkManager, AlarmManager |
| Permisos del navegador | Permisos de Android (más estrictos y variados) |

Ruta de aprendizaje sugerida:
1. **Kotlin básico** — kotlinlang.org "Kotlin Tour" (2–4 días).
2. **Curso oficial gratuito "Android Basics with Compose"** de Google
   (developer.android.com/courses) — unidades 1 a 6 (1–2 semanas).
3. **Room + ViewModel + Navigation** (incluido en el curso, unidades 5–6).
4. Instalar **Android Studio** y usar el emulador + un teléfono real con
   "depuración USB" activada.

### Fase 0 — Prototipo técnico (2–3 semanas)
Objetivo: comprobar que lo difícil funciona.
- [ ] Burbuja flotante que sobrevive en Samsung y Xiaomi (los peores casos).
- [ ] Mini-panel de captura que guarda en Room.
- [ ] Un recordatorio exacto que suene con la app cerrada.

### Fase 1 — MVP (10–14 semanas)
- [ ] Elegir modo: **cuenta Google** o **perfil local con nombre**.
- [ ] Respaldo automático en la nube para cuentas Google.
- [ ] Onboarding de permisos (burbuja, notificaciones, alarmas, batería).
- [ ] Burbuja: captura de texto, mover, ocultar, ajustes básicos.
- [ ] Bandeja, Notas, Tareas (con checklist), Recordatorios.
- [ ] Parser de fechas en español ("mañana a las 5").
- [ ] Pantalla "Hoy" y búsqueda.
- [ ] Widgets: **Captura rápida** y **Mi día**.
- [ ] Quick Settings Tile + compartir a Hilvana.
- [ ] Tema claro/oscuro + colores dinámicos.
- [ ] Exportar/importar respaldo.
- [ ] Publicación en Play (prueba cerrada → producción).

### Fase 2 — Crecimiento
- [ ] Notas de voz desde la burbuja (mantener presionado).
- [ ] Resto de widgets (Próximo recordatorio, Nota fijada, Semana).
- [ ] Sincronización entre varios dispositivos + versión web de solo lectura.
- [ ] Bloqueo con huella/PIN.
- [ ] Tareas repetitivas avanzadas, subtareas, prioridades.
- [ ] Adjuntar fotos; capturar texto de pantalla (OCR).

### Fase 3 — Diferenciación fuerte
- [ ] Auto-clasificación con IA (detecta si es tarea, idea o recordatorio, y
      sugiere etiquetas).
- [ ] Recordatorios por ubicación ("cuando llegue al súper").
- [ ] Resumen diario/semanal ("Esto quedó pendiente").
- [ ] Listas compartidas con familia/pareja.

### Fase 4 — iPhone (si la app funciona bien en Android)
- [ ] Reutilizar la lógica con Kotlin Multiplatform; interfaz en SwiftUI o
      Compose Multiplatform.
- [ ] En iOS **no hay burbuja flotante**: la captura rápida será con widgets,
      Atajos de Siri, el Botón de Acción y widgets en la pantalla de bloqueo.

---

## 9. Modelo de negocio (sugerencia)

**Decisión: 100% gratis al lanzar.** Si la app crece, se agregan funciones
extra y algunas pueden ser de pago (freemium), sin anuncios (los anuncios en una
app de notas personales dan mala imagen y espantan usuarios).

- **Siempre gratis**: burbuja, notas, tareas, recordatorios, widgets básicos,
  cuenta Google con respaldo, perfil local.
- **Posible "Hilvana Pro" a futuro**: IA (auto-clasificación, resúmenes),
  recordatorios por ubicación, listas compartidas, temas extra, adjuntos grandes.

Reglas para no arrepentirse después:
- **Nunca quitar** algo que ya era gratis para cobrarlo — genera malas reseñas.
  Lo de pago deben ser funciones **nuevas**.
- Desde el MVP, tener un "interruptor" interno de funciones (feature flags) para
  que agregar Pro después no requiera reescribir la app.
- Cuando llegue el momento, el cobro se hace con **Google Play Billing**
  (Google se queda con 15% de los primeros USD 1M al año).

## 10. Métricas para saber si funciona

- % de usuarios que **activan la burbuja** en el onboarding (meta: >50%).
- Capturas por usuario por día (meta: ≥3).
- % de capturas hechas desde burbuja/widget vs. desde la app.
- Retención día 7 y día 30.
- Recordatorios que suenan a tiempo (medir fallos por fabricante).

## 11. Riesgos principales

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Usuarios no dan el permiso de superposición | Alto | Onboarding claro + plan B de captura (7.5) |
| Fabricantes matan el servicio | Alto | Guías por marca, reinicio automático, pruebas en Xiaomi/Samsung |
| Google Play rechaza el uso de `specialUse` / superposición | Medio | Justificación clara + video demo de la función principal |
| La burbuja resulta molesta | Medio | Ocultar en pantalla completa, transparencia, ocultar temporalmente |
| Mercado saturado de apps de notas | Medio | Enfocar el marketing en "capturar en 2 segundos", no en "otra app de notas" |
| Pérdida de datos | Alto | Offline-first con Room + respaldos desde el MVP |

---

## 12. Recomendaciones finales

1. **Empezar por el prototipo de la burbuja** (Fase 0). Es lo más riesgoso
   técnicamente; si funciona bien, todo lo demás es estándar.
2. **Probar en teléfonos reales** de las marcas más usadas por tu público
   (en Latinoamérica: Samsung, Xiaomi, Motorola).
3. **Captura primero, organización después.** Resiste la tentación de llenar el
   panel de captura de opciones; el valor está en que sea instantáneo.
4. **MVP pequeño y pulido** > muchas funciones a medias.
5. Pide feedback temprano a 10–20 personas reales durante la prueba cerrada de
   Play.

---

## 13. Decisiones tomadas

- ✅ **Plataforma**: solo Android por ahora; iPhone después si la app funciona.
- ✅ **Cuentas**: dos modos — cuenta Google (con respaldo en la nube) o perfil
  local con el nombre que el usuario escriba.
- ✅ **Precio**: gratis al lanzar; funciones de pago nuevas más adelante si crece.
- ✅ **Experiencia**: experiencia en web y software, nuevo en Android → se agrega
  la Fase A de aprendizaje y se ajustan los tiempos.

## 14. Preguntas abiertas (por decidir)

- [ ] ¿Idioma: solo español al inicio o español + inglés?
- [ ] ¿Identidad visual: colores, logo, estilo de la burbuja?
- [ ] ¿Qué teléfono(s) Android tienes para probar? (marca y versión)
