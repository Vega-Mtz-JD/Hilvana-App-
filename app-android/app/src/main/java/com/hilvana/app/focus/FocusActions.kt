package com.hilvana.app.focus

import android.content.Context
import com.hilvana.app.data.Item
import com.hilvana.app.data.nextStep
import com.hilvana.app.data.toggled
import com.hilvana.app.data.withNextStepDone
import com.hilvana.app.repository

/**
 * Lo que se puede hacer con una tarea en foco desde fuera de la app (notificación,
 * burbuja, widget). Si se marca el último paso, la tarea queda hecha.
 */
object FocusActions {

    suspend fun completeNextStep(context: Context, item: Item) =
        saveSteps(context, item, item.checklist.withNextStepDone())

    suspend fun toggleStep(context: Context, item: Item, index: Int) =
        saveSteps(context, item, item.checklist.toggled(index))

    suspend fun complete(context: Context, item: Item) {
        context.repository.save(item.copy(done = true))
    }

    private suspend fun saveSteps(context: Context, item: Item, steps: List<com.hilvana.app.data.CheckItem>) {
        val allDone = steps.isNotEmpty() && steps.nextStep == null
        context.repository.save(item.copy(checklist = steps, done = allDone))
    }
}
