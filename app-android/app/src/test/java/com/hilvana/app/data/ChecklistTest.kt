package com.hilvana.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChecklistTest {

    private val steps = listOf(
        CheckItem("Empacar libros", done = true),
        CheckItem("Llamar al flete"),
        CheckItem("Cambiar dirección"),
    )

    @Test fun idaYVueltaJson() = assertEquals(steps, ChecklistJson.decode(ChecklistJson.encode(steps)))

    @Test fun textoVacioOMaloEsListaVacia() {
        assertEquals(emptyList<CheckItem>(), ChecklistJson.decode(null))
        assertEquals(emptyList<CheckItem>(), ChecklistJson.decode("no es json"))
    }

    @Test fun avanceYSiguientePaso() {
        assertEquals("1/3", steps.progressText)
        assertEquals("Llamar al flete", steps.nextStep?.text)
    }

    @Test fun marcarSiguientePaso() {
        val after = steps.withNextStepDone()
        assertEquals("2/3", after.progressText)
        assertEquals("Cambiar dirección", after.nextStep?.text)
        assertNull(after.withNextStepDone().nextStep)
    }

    @Test fun alternarUnPaso() = assertEquals(false, steps.toggled(0)[0].done)
}
