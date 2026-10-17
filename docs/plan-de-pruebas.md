# Plan de pruebas de Hilvana

Recorre esta lista en tu celular. Marca `[x]` lo que funcione y anota lo que no
(qué hiciste, qué esperabas, qué pasó). Lo marcado ✅ ya lo confirmaste.

> Consejo: instala la última versión con `.\herramientas\probar.cmd` antes de empezar.

---

## 1. Lo esencial (ya confirmado)

- ✅ Burbuja flotante: aparece, se mueve, se pega al borde, se oculta en la ✕.
- ✅ Panel de captura: pegar, imagen, calendario y hora, tipo arriba con colores.
- ✅ Recordatorio suena con la pantalla bloqueada y con la app cerrada.
- ✅ Pantallas Hoy / Bandeja / Notas / Tareas / Calendario y editor.
- ✅ Carpetas de notas, imágenes en notas.
- ✅ Notas de voz (dictar y grabar), vista previa de links, meta diaria.
- ✅ Conectar con Google y primer respaldo en Drive.

## 2. Pendiente: captura sin burbuja

- [ ] **Compartir texto**: en Chrome, comparte una página → Hilvana → guarda.
- [ ] **Compartir imagen**: en Galería, comparte una foto/captura → Hilvana → guarda.
- [ ] **Mosaico de Ajustes rápidos**: Ajustes → "Agregar mosaico" (o agrégalo a mano) → bájalo y tócalo.
- [ ] **Acceso directo**: mantén presionado el ícono de Hilvana → Capturar.
- [ ] **Mantener presionada la burbuja** → se abre la grabación de voz.

## 3. Pendiente: recordatorios y tareas

- [ ] **Reiniciar el teléfono** con un recordatorio de 10 min pendiente → debe sonar igual sin abrir Hilvana.
- [ ] Notificación de recordatorio → **Posponer 15 min** → vuelve a sonar 15 min después.
- [ ] Notificación de recordatorio → **✓ Hecho** → queda completado.
- [ ] **Repetitivo**: "todos los días a las [en 2 min] prueba" → suena, y mañana vuelve a sonar sin tocar nada.
- [ ] **Tarea que se repite** con fecha de hoy → márcala → salta a la próxima fecha y queda un registro en Completadas.
- [ ] **Estrellas**: "pagar luz urgente" desde la burbuja → tarea con ★★★★★, aparece arriba en Tareas.
- [ ] **Captura inteligente**: "el viernes a las 8 de la noche cena", "15/11 cumpleaños", "links: https://…".

## 4. Pendiente: tareas en foco 🎯

- [ ] Tarea con 2–3 pasos → activa 🎯 En foco → aparece la **notificación fija** (también en la pantalla de bloqueo).
- [ ] En la notificación, **✓ siguiente paso** → el avance cambia; al último, la tarea queda hecha.
- [ ] **Globito** en la burbuja con el número de tareas en foco; franja 🎯 arriba del panel.
- [ ] **Empujoncitos** cada 1 h → llega un aviso (no entre 22:00 y 8:00).

## 5. Pendiente: widgets

- [ ] **Captura rápida** (4×1): abre el panel.
- [ ] **Mi día** (4×2): marca una tarea desde el widget; muestra la meta "2/3".
- [ ] **🎯 En foco**: marca un paso desde el widget.

## 6. Pendiente: privacidad y respaldo

- [ ] **Carpeta protegida** (🔑 Claves): sal 1 min y vuelve → pide huella; sus notas no aparecen en "Todas" ni en Bandeja ni en Buscar.
- [ ] **Bloquear Hilvana al abrir** (Ajustes → Privacidad) → pide huella; widget Mi día solo muestra "🔒 N pendientes".
- [ ] **Exportar** respaldo a Drive/Descargas → **Importar** el mismo → "X ya estaban" (no duplica).
- [ ] **Restaurar desde Drive** → "X ya estaban".
- [ ] **Prueba de fuego** ⚠️ (haz antes un respaldo en Drive): desinstala Hilvana → instálala de nuevo con `probar.cmd` → Conectar con Google → **Restaurar** → vuelven tus notas, imágenes y audios.
- [ ] Al día siguiente: en Ajustes, la fecha del "Último respaldo en Drive" se actualizó sola.

## 7. Pendiente: búsqueda, estadísticas y diseño

- [ ] 🔍 Buscar sin acentos ("cancion" → "Canción"); filtro 🔗 Con enlace; palabra de un paso del checklist.
- [ ] 📊 Estadísticas: actividad de 14 días, por tipo, por importancia, racha.
- [ ] **Ícono** nuevo en la pantalla de inicio y el cajón de apps; con íconos temáticos activados (si tu launcher los tiene).
- [ ] **Tema claro** (Ajustes → Apariencia) y **colores del fondo de pantalla**.
- [ ] **Bienvenida**: Ajustes → "Ver la bienvenida otra vez" → recorre todos los pasos.

## 8. Pendiente: resistencia (Xiaomi)

- [ ] Deja la burbuja activa **24 horas** usando el celular normal: ¿sigue ahí?
- [ ] Limpia las apps recientes: ¿la burbuja y la notificación de foco sobreviven?
- [ ] Sin internet (modo avión): capturar, buscar y ver notas funciona; la vista previa de links se completa al volver la conexión.

---

### Cómo reportar un problema

Dile a Claude: **qué paso de esta lista**, qué hiciste, qué esperabas y qué pasó.
Con el celular conectado, Claude puede leer el registro de errores de la app.
