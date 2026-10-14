package com.hilvana.app.data

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

/** Un paso del checklist de una tarea. */
data class CheckItem(val text: String, val done: Boolean = false)

/** Avance del checklist: "2/5". */
val List<CheckItem>.progressText: String get() = "${count { it.done }}/$size"

/** El primer paso que falta, o null si ya están todos (o no hay checklist). */
val List<CheckItem>.nextStep: CheckItem? get() = firstOrNull { !it.done }

/** Marca el siguiente paso pendiente como hecho. */
fun List<CheckItem>.withNextStepDone(): List<CheckItem> {
    val index = indexOfFirst { !it.done }
    return if (index < 0) this else toggled(index)
}

fun List<CheckItem>.toggled(index: Int): List<CheckItem> =
    mapIndexed { i, step -> if (i == index) step.copy(done = !step.done) else step }

/** El checklist se guarda como texto JSON en una sola columna: [{"t":"…","d":true}, …]. */
object ChecklistJson {
    fun encode(steps: List<CheckItem>): String =
        JSONArray(steps.map { JSONObject().put("t", it.text).put("d", it.done) }).toString()

    fun decode(text: String?): List<CheckItem> {
        if (text.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { CheckItem(it.optString("t"), it.optBoolean("d")) }
        }
    }
}

class Converters {
    @TypeConverter fun checklistToText(steps: List<CheckItem>): String = ChecklistJson.encode(steps)
    @TypeConverter fun textToChecklist(text: String?): List<CheckItem> = ChecklistJson.decode(text)
}
