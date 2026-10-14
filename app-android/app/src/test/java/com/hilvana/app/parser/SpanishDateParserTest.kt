package com.hilvana.app.parser

import com.hilvana.app.data.ItemType
import com.hilvana.app.data.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class SpanishDateParserTest {

    // "Ahora" fijo: lunes 12 de octubre de 2026, 10:00.
    private val now = LocalDateTime.of(2026, 10, 12, 10, 0)

    private fun check(input: String, expectedText: String, expectedDue: LocalDateTime?) {
        val result = SpanishDateParser.parse(input, now)
        assertEquals("texto de \"$input\"", expectedText, result.text)
        assertEquals("fecha de \"$input\"", expectedDue, result.dueAt)
    }

    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, month, day, hour, minute)

    @Test fun mananaALas5() = check("mañana a las 5 llamar a mamá", "Llamar a mamá", at(10, 13, 17))
    @Test fun horaPmAlFinal() = check("llamar a mamá mañana 5pm", "Llamar a mamá", at(10, 13, 17))
    @Test fun sinFecha() = check("comprar pan", "Comprar pan", null)
    @Test fun aMamaNoEsHora() = check("llamar a mamá", "Llamar a mamá", null)
    @Test fun enMinutos() = check("en 10 minutos sacar la ropa", "Sacar la ropa", at(10, 12, 10, 10))
    @Test fun enMediaHora() = check("en media hora revisar el horno", "Revisar el horno", at(10, 12, 10, 30))
    @Test fun dentroDeHoras() = check("dentro de 2 horas reunión", "Reunión", at(10, 12, 12))
    @Test fun enUnaHora() = check("pagar en una hora", "Pagar", at(10, 12, 11))
    @Test fun viernesDeLaNoche() =
        check("el viernes a las 8 de la noche cena", "Cena", at(10, 16, 20))
    @Test fun diaYMes() = check("pagar luz el 15 de octubre", "Pagar luz", at(10, 15, 9))
    @Test fun horaQueYaPasoEsManana() = check("a las 9 tomar pastilla", "Tomar pastilla", at(10, 13, 9))
    @Test fun horaAmbiguaEsDeLaTarde() = check("a las 3 cita", "Cita", at(10, 12, 15))
    @Test fun mananaEnLaManana() = check("mañana en la mañana gimnasio", "Gimnasio", at(10, 13, 9))
    @Test fun pasadoManana() = check("pasado mañana dentista", "Dentista", at(10, 14, 9))
    @Test fun hoyConMinutos() = check("hoy a las 18:30 llamar a Juan", "Llamar a Juan", at(10, 12, 18, 30))
    @Test fun mismoDiaDeLaSemanaEsLaProxima() = check("el lunes junta", "Junta", at(10, 19, 9))
    @Test fun diaBarraMes() = check("15/11 cumpleaños de Ana", "Cumpleaños de Ana", at(11, 15, 9))
    @Test fun estaNoche() = check("esta noche ver película", "Ver película", at(10, 12, 20))
    @Test fun soloParteDelDia() = check("en la tarde lavar el carro", "Lavar el carro", at(10, 12, 15))
    @Test fun mediodia() = check("almuerzo con Sofi al mediodía", "Almuerzo con Sofi", at(10, 12, 12))
    @Test fun fechaPasadaEsElProximoAno() =
        check("aniversario 3 de enero", "Aniversario", LocalDateTime.of(2027, 1, 3, 9, 0))

    @Test fun recordatorioSiHayFecha() {
        assertEquals(ItemType.REMINDER, SpanishDateParser.parse("mañana pagar renta", now).type)
    }

    @Test fun prefijoDeCarpeta() {
        val result = SpanishDateParser.parse("ideas: app de recetas", now)
        assertEquals("Ideas", result.prefix)
        assertEquals("App de recetas", result.textAfterPrefix)
        assertNull(result.type)
        assertNull(result.dueAt)
    }

    @Test fun prefijoConFecha() {
        val result = SpanishDateParser.parse("compras: mañana leche", now)
        assertEquals("Compras", result.prefix)
        assertEquals("Leche", result.textAfterPrefix)
        assertEquals(at(10, 13, 9), result.dueAt)
    }

    @Test fun sinPrefijo() {
        assertNull(SpanishDateParser.parse("comprar pan", now).prefix)
    }

    @Test fun enlaceConPrefijo() {
        val result = SpanishDateParser.parse("links: https://youtu.be/abc123", now)
        assertEquals("Links", result.prefix)
        assertEquals("https://youtu.be/abc123", result.textAfterPrefix)
    }

    private fun priorityOf(input: String) = SpanishDateParser.parse(input, now)

    @Test fun urgenteEsCincoEstrellas() {
        val r = priorityOf("pagar luz urgente")
        assertEquals(5, r.priority)
        assertEquals("Pagar luz", r.text)
    }

    @Test fun importanteEsCuatroYConFecha() {
        val r = priorityOf("mañana importante revisar contrato")
        assertEquals(4, r.priority)
        assertEquals("Revisar contrato", r.text)
        assertEquals(at(10, 13, 9), r.dueAt)
    }

    @Test fun muyImportanteEsCinco() = assertEquals(5, priorityOf("muy importante: llamar al banco").priority)

    @Test fun signosDeExclamacion() {
        val r = priorityOf("llamar al banco!!!")
        assertEquals(3, r.priority)
        assertEquals("Llamar al banco", r.text)
    }

    @Test fun unSoloSignoPegadoNoEsImportancia() {
        val r = priorityOf("qué buena idea!")
        assertNull(r.priority)
        assertEquals("Qué buena idea!", r.text)
    }

    private fun checkRepeat(input: String, text: String, due: LocalDateTime, repeat: RepeatRule) {
        val r = SpanishDateParser.parse(input, now)
        assertEquals("texto de \"$input\"", text, r.text)
        assertEquals("fecha de \"$input\"", due, r.dueAt)
        assertEquals("repetición de \"$input\"", repeat, r.repeat)
    }

    // Recordar: "ahora" es lunes 12 de octubre de 2026, 10:00.
    @Test fun todosLosDias() =
        checkRepeat("todos los días a las 8 tomar pastilla", "Tomar pastilla", at(10, 13, 8), RepeatRule.DAILY)

    @Test fun todosLosLunesEsHoySiAunNoPasa() =
        checkRepeat("todos los lunes a las 7 gimnasio", "Gimnasio", at(10, 12, 19), RepeatRule.WEEKLY)

    @Test fun cadaMesElDia5() =
        checkRepeat("pagar renta cada mes el 5", "Pagar renta", at(11, 5, 9), RepeatRule.MONTHLY)

    @Test fun deLunesAViernes() =
        checkRepeat("de lunes a viernes a las 9 revisar correo", "Revisar correo", at(10, 13, 9), RepeatRule.WEEKDAYS)

    @Test fun reunionSemanal() =
        checkRepeat("reunión semanal el viernes a las 10", "Reunión", at(10, 16, 10), RepeatRule.WEEKLY)

    @Test fun cadaDomingoEnPlural() =
        checkRepeat("todos los domingos llamar a la abuela", "Llamar a la abuela", at(10, 18, 9), RepeatRule.WEEKLY)

    @Test fun miDiarioNoEsRepeticion() {
        val r = SpanishDateParser.parse("escribir en mi diario", now)
        assertNull(r.repeat)
        assertNull(r.dueAt)
    }

    @Test fun losEnlacesNoSonFechas() =
        check("video https://tiktok.com/@ana/video/12/10", "Video https://tiktok.com/@ana/video/12/10", null)
}
